/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.util;

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.RequestThreadLocal;
import org.artifactory.rest.ErrorResponse;
import org.artifactory.util.encoding.URIUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.jfrog.build.api.Build;
import org.jfrog.client.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.*;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author yoavl
 */
public abstract class HttpUtils {

    public static final String WEBAPP_URL_PATH_PREFIX = "webapp";
    private static final String BROWSE_REPO_URL_PREFIX = "/#/artifacts/browse/tree/General/";

    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private static String userAgent;

    private HttpUtils() {
        // utility class
    }

    public static String getArtifactoryUserAgent() {
        if (userAgent == null) {
            String artifactoryVersion = ConstantValues.artifactoryVersion.getString();
            if (artifactoryVersion.startsWith("$") || artifactoryVersion.endsWith("SNAPSHOT")) {
                artifactoryVersion = "development";
            }
            userAgent = "Artifactory/" + artifactoryVersion;
        }
        return userAgent;
    }

    /**
     * Reset the cached Artifactory user agent string (required after upgrade)
     */
    public static void resetArtifactoryUserAgent() {
        userAgent = null;
    }

    @SuppressWarnings({"IfMayBeConditional"})
    public static String getRemoteClientAddress(HttpServletRequest request) {
        String remoteAddress;
        //Check if there is a remote address coming from a proxied request
        //(http://httpd.apache.org/docs/2.2/mod/mod_proxy.html#proxypreservehost)
        String header = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(header)) {
            //Might contain multiple entries - take the first
            remoteAddress = new StringTokenizer(header, ",").nextToken();
        } else {
            //Take it the standard way
            remoteAddress = request.getRemoteAddr();
        }
        return remoteAddress;
    }

    public static String getServletContextUrl(HttpServletRequest httpRequest) {
        return getServletContextUrl(ContextHelper.get(), httpRequest);
    }

    public static String getServletContextUrl(ArtifactoryContext artifactoryContext, HttpServletRequest httpRequest) {
        String origUrl = httpRequest.getHeader(ArtifactoryRequest.ARTIFACTORY_OVERRIDE_BASE_URL);
        if (StringUtils.isNotBlank(origUrl)) {
            // original artifactory request url overrides request and base url
            return origUrl;
        }
        CentralConfigService centralConfigService = artifactoryContext.getCentralConfig();
        String baseUrl = centralConfigService.getDescriptor().getUrlBase();
        if (!StringUtils.isEmpty(baseUrl)) {
            String scheme = httpRequest.getScheme();
            if (baseUrl.startsWith(scheme)) {
                return baseUrl;
            } else {
                int idx = baseUrl.indexOf("://");
                if (idx > 0) {
                    return scheme + "://" + baseUrl.substring(idx + 3);
                } else {
                    return scheme + "://" + baseUrl;
                }
            }
        }
        return getServerUrl(httpRequest) + httpRequest.getContextPath();
    }

    public static String getRestApiUrl(HttpServletRequest request) {
        return getServletContextUrl(request) + "/" + RestConstants.PATH_API;
    }

    public static String getServerUrl(HttpServletRequest httpRequest) {
        int port = httpRequest.getServerPort();
        String scheme = httpRequest.getScheme();
        if (isDefaultPort(scheme, port)) {
            return scheme + "://" + httpRequest.getServerName();
        }
        return scheme + "://" + httpRequest.getServerName() + ":" + port;
    }

    public static boolean isDefaultPort(String scheme, int port) {
        switch (port) {
            case 80:
                return "http".equalsIgnoreCase(scheme);
            case 443:
                return "https".equalsIgnoreCase(scheme);
            default:
                return false;
        }
    }

    public static String getSha1Checksum(ArtifactoryRequest request) {
        String sha1Header = request.getHeader(ArtifactoryRequest.CHECKSUM_SHA1);
        return sha1Header != null ? sha1Header : getChecksum(ChecksumType.sha1, request);
    }

    public static String getSha256Checksum(ArtifactoryRequest request) {
        String sha2Header = request.getHeader(ArtifactoryRequest.CHECKSUM_SHA256);
        return sha2Header != null ? sha2Header : getChecksum(ChecksumType.sha256, request);
    }

    public static String getMd5Checksum(ArtifactoryRequest request) {
        String md5Header = request.getHeader(ArtifactoryRequest.CHECKSUM_MD5);
        return md5Header != null ? md5Header : getChecksum(ChecksumType.md5, request);
    }

    private static String getChecksum(ChecksumType checksumType, ArtifactoryRequest request) {
        String checksumHeader = request.getHeader(ArtifactoryRequest.CHECKSUM);
        //TODO [by dan]: should probably test for validity as soon as here, no time for behavior changes now :(
        if (StringUtils.isNotBlank(checksumHeader) && checksumHeader.length() == checksumType.length()) {
            return checksumHeader;
        }
        return null;
    }

    public static boolean isExpectedContinue(ArtifactoryRequest request) {
        String expectHeader = request.getHeader("Expect");
        if (StringUtils.isBlank(expectHeader)) {
            return false;
        }
        // some clients make the C lowercase even when passed uppercase
        return expectHeader.contains("100-continue") || expectHeader.contains("100-Continue");
    }

    public static String getContextId(ServletContext servletContext) {
        return PathUtils.trimLeadingSlashes(servletContext.getContextPath());
    }

    /**
     * @param status The (http based) response code
     * @return True if the code symbols a successful request cycle (i.e., in the 200-299 range)
     */
    public static boolean isSuccessfulResponseCode(int status) {
        return HttpStatus.SC_OK <= status && status <= 299;
    }

    /**
     * @param status The (http based) response code
     * @return True if the code symbols a successful request cycle (i.e., in the 300-399 range)
     */
    public static boolean isRedirectionResponseCode(int status) {
        return HttpStatus.SC_MULTIPLE_CHOICES <= status && status <= 399;
    }

    /**
     * Calculate a unique id for the VM to support Artifactories with the same ip (e.g. accross NATs)
     */
    public static String getHostId() {
        return ArtifactoryHome.get().getHostId();
    }

    /**
     * @param response The response to get the body from
     * @return Returns the response body input stream or null is there is none.
     */
    @Nullable
    public static InputStream getResponseBody(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        return entity == null ? null : entity.getContent();
    }

    public static String encodeQuery(String unescaped) {
        try {
            return URIUtil.encodeQuery(unescaped, "UTF-8");
        } catch (HttpException e) {
            // Nothing to do here, we will return the un-escaped value.
            log.warn("Could not encode path '{}' with UTF-8 charset, returning the un-escaped value.", unescaped);
        }
        return unescaped;
    }

    public static String encodeWithinPath(String unescaped) {
        try {
            return URIUtil.encodeWithinPath(unescaped);
        } catch (HttpException e) {
            // Nothing to do here, we will return the un-escaped value.
            log.warn("Could not encode path '{}' with UTF-8 charset, returning the un-escaped value.", unescaped);
        }
        return unescaped;
    }


    public static String decodeUri(String encodedUri) {
        try {
            return URIUtil.decode(encodedUri, "UTF-8");
        } catch (HttpException e) {
            // Nothing to do here, we will return the un-escaped value.
            log.warn("Could not decode uri '{}' with UTF-8 charset, returning the encoded value.", encodedUri);
        }
        return encodedUri;
    }

    public static String decodeUrlWithURLDecoder(String url) {
        try {
            return URLDecoder.decode(url, CharEncoding.UTF_8);
        } catch (UnsupportedEncodingException e) {
            log.error("Could not decode uri '{}' with UTF-8 charset, returning the encoded value.", url);
        }
        return url;
    }

    public static String encodeUrlWithURLEncoder(String url) {
        try {
            return URLEncoder.encode(url, CharEncoding.UTF_8);
        } catch (UnsupportedEncodingException e) {
            log.error("Could not decode uri '{}' with UTF-8 charset, returning the encoded value.", url);
        }
        return url;
    }

    /**
     * Removes the query parameters from the given url
     *
     * @param url URL string with query parameters, e.g. "http://hello/world?lang=java&run=1"
     * @return new string object without the query parameters, e.g. "http://hello/world". If no query elements found the
     * original string is returned.
     */
    public static String stripQuery(String url) {
        int i = url.indexOf("?");
        if (i > -1) {
            return url.substring(0, i);
        } else {
            return url;
        }
    }

    public static String adjustRefererValue(Map<String, String> headersMap, String headerVal) {
        //Append the artifactory user agent to the referer
        if (headerVal == null) {
            //Fallback to host
            headerVal = headersMap.get("HOST");
            if (headerVal == null) {
                //Fallback to unknown
                headerVal = "UNKNOWN";
            }
        }
        if (!headerVal.startsWith("http")) {
            headerVal = "http://" + headerVal;
        }
        try {
            java.net.URL uri = new java.net.URL(headerVal);
            //Only use the uri up to the path part
            headerVal = uri.getProtocol() + "://" + uri.getAuthority();
        } catch (MalformedURLException e) {
            //Nothing
        }
        headerVal += "/" + HttpUtils.getArtifactoryUserAgent();
        return headerVal;
    }

    /**
     * Extracts the content length from the response header, or return -1 if the content-length field was not found.
     *
     * @param response The response
     * @return Content length in bytes or -1 if header not found
     */
    public static long getContentLength(HttpResponse response) {
        Header contentLengthHeader = response.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
        if (contentLengthHeader != null) {
            return extractContentLengthFromHeader(contentLengthHeader.getValue());
        } else {
            return -1;
        }
    }

    public static String getServerAndPortFromContext(String contextUrl) {
        String[] splittedServerContext = contextUrl.split("/");
        if (splittedServerContext.length >= 3) {
            return splittedServerContext[2];
        } else {
            return "";
        }
    }

    public static String getApacheServerAndPortFromContext(String contextUrl) {
        String[] splittedServerContext = contextUrl.split("/");
        if (splittedServerContext.length >= 3) {
            return splittedServerContext[0] + "//" + splittedServerContext[2];
        } else {
            return "";
        }
    }

    /**
     * Return content length as long (required for uploaded files > 2GB).
     * The servlet api can only return this as int.
     *
     * @param request The request
     * @return The content length in bytes or -1 if not found
     */
    public static long getContentLength(HttpServletRequest request) {
        return extractContentLengthFromHeader(request.getHeader(HttpHeaders.CONTENT_LENGTH));
    }

    private static long extractContentLengthFromHeader(String lengthHeader) {
        long contentLength;
        if (lengthHeader != null) {
            try {
                contentLength = Long.parseLong(lengthHeader);
            } catch (NumberFormatException e) {
                log.trace("Bad Content-Length value {}", lengthHeader);
                contentLength = -1;
            }
        } else {
            contentLength = -1;
        }
        return contentLength;
    }

    public static void sendErrorResponse(HttpServletResponse response, int statusCode, String message)
            throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(statusCode);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        ErrorResponse errorResponse = new ErrorResponse(statusCode, message);
        response.getWriter().write(mapper.writeValueAsString(errorResponse));
    }

    public static boolean isAbsolute(String url) {
        try {
            URI uri = new URIBuilder(url).build();
            return uri.isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static String getRemoteClientAddress() {
        if (ConstantValues.test.getBoolean()) {
            return "127.0.0.1";
        }
        String remoteClientAddress = RequestThreadLocal.getClientAddress();
        if (remoteClientAddress == null) {
            return "";
        } else {
            return remoteClientAddress;
        }
    }

    public static String resolveResponseRemoteAddress(CloseableHttpResponse response) {
        try {
            Field connHolderField = response.getClass().getDeclaredField("connHolder");
            connHolderField.setAccessible(true);
            Object connHolder = connHolderField.get(response);

            Field managedConnField = connHolder.getClass().getDeclaredField("managedConn");
            managedConnField.setAccessible(true);
            ManagedHttpClientConnection managedConn = (ManagedHttpClientConnection) managedConnField.get(
                    connHolder);
            String hostAddress = managedConn.getSocket().getInetAddress().getHostAddress();
            return hostAddress == null ? StringUtils.EMPTY : hostAddress;
        } catch (Throwable throwable) {
            return StringUtils.EMPTY;
        }
    }

    public static String createBuildInfoLink(Build build) {
        String artifactoryUrl = ContextHelper.get().beanForType(
                CentralConfigService.class).getDescriptor().getServerUrlForEmail();
        if (StringUtils.isBlank(artifactoryUrl)) {
            return build.getName() + ":" + build.getNumber();
        } else {
            try {
                String href = Joiner.on("/").join(
                        artifactoryUrl + HttpUtils.WEBAPP_URL_PATH_PREFIX,
                        "builds",
                        // Do a manual "encoding" of spaces for the build name. This is due to the fact that if the mail
                        // is sent to a Gmail account it will automatically insert '+' for every space, and not its '%20'
                        // hex representation, this will cause a broken link. see more here:
                        // http://www.google.fr/support/forum/p/gmail/thread?tid=53a5c616a0324d96&hl=en
                        build.getName().replace(" ", "%20"),
                        build.getNumber());

                return "<a href=\"" + href + "\"" + " target=\"blank\">" + build.getName() + ":" + build.getNumber()
                        + "</a>";
            } catch (Exception e) {
                return build.getName() + ":" + build.getNumber();
            }
        }
    }

    public static String createLinkToBrowsableArtifact(RepoPath repoPath, String linkLabel) {
        String artifactoryUrl = ContextHelper.get().beanForType(CentralConfigService.class).getDescriptor()
                .getServerUrlForEmail();
        if (StringUtils.isBlank(artifactoryUrl)) {
            return linkLabel;
        } else {
            String url = artifactoryUrl + HttpUtils.WEBAPP_URL_PATH_PREFIX + BROWSE_REPO_URL_PREFIX
                    + HttpUtils.encodeQuery(repoPath.toPath());
            return "<a href=" + url + " target=\"blank\"" + ">" + linkLabel + "</a>";
        }
    }

    /**
     * Extracts session access time if session exist,
     * if session is null, returns System.currentTimeMillis
     *
     * @return session access time
     */
    public static long getSessionAccessTime(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        return session != null ?
                (
                        session.getLastAccessedTime() == 0 ?
                                session.getCreationTime()
                                :
                                session.getLastAccessedTime()
                )
                :
                System.currentTimeMillis();
    }

    /**
     * Checks whether request targeted for changePassword api
     * or if admin triggered "unExpirePasswordForAllUsers" action
     *
     * @return true if request targeted for "changePassword"
     */
    public static boolean isChangePasswordRequest(ServletRequest servletRequest) {
        String uri = ((HttpServletRequest) servletRequest).getRequestURI();
        String invokedMethod = PathUtils.getLastPathElement(uri);
        return "changePassword".equals(invokedMethod) || "unExpirePasswordForAllUsers".equals(invokedMethod);
    }
}
