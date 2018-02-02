var gulp = require("gulp");
var gutil = require("gulp-util");
var webpack = require("webpack");
var path = require('path');
var concat = require('gulp-concat');
var html2js = require('gulp-html2js');
var webpackConfig = require('./webpack.config');
var less = require('gulp-less');
var sourceMaps = require('gulp-sourcemaps');
var CONFIG = require('./artifactory.config');
var install = require("gulp-install");
var iconfont = require('gulp-iconfont');
var iconfontCss = require('gulp-iconfont-css');
var browserSync = require('browser-sync');
var runSequence = require('run-sequence');
var reload      = browserSync.reload;
var prefixer = require('gulp-autoprefixer');
var combiner = require('stream-combiner2');
var uglify = require('gulp-uglify');
var minifyCss = require('gulp-minify-css');
var RevAll = require('gulp-rev-all');
var revReplace = require("gulp-rev-replace");
var revNapkin = require('gulp-rev-napkin');
var karma = require('karma');
var rimraf = require('gulp-rimraf');
var replace = require('gulp-replace');
var fs = require('fs');


const proxy = require('http-proxy-middleware');

// default task runs the development tasks seq
gulp.task('default',['build', 'watch']);


// Build tasks common to dev and production
gulp.task('build:common',
    function(callback) {
        runSequence(
            'clean',
            [
                'webpack',
                'templates',
                'vendorScripts',
                'vendorStyles',
                'vendorStylesAssets',
                'vendorFonts',
                'less',
                'copyHtml',
                'fonts',
                'images',
                'jqueryui-images',
                'jfrog-ui-images',
                'jfrog-ui-fonts'
            ],
            callback
        );
    }
);


// Build the client. This is run by Jenkins
gulp.task('build',
    function(callback) {
        delete webpackConfig.devtool; //don't generate source maps on production build
        runSequence(
            'build:common',
            'revreplace',
            callback
        );
    }
);

// Same as build:common just copy styleguide
gulp.task("build:dev",
    function(callback) {
        runSequence(
            'build:common',
            'copyStyleguide',
            callback
        );
    }
);

// Clean up
gulp.task('clean', function() {
    return gulp.src(CONFIG.DESTINATIONS.TARGET, { read: false })
        .pipe(rimraf({ force: true }));
});

// Reload everything:
gulp.task("reload", reload);
// Reload CSS files:
gulp.task("reloadCss", function() {
    reload(["vendorStyles.css", "application.css"]);
});

// Utility factory function to create a function that runs a sequence
function sequence() {
    var args = arguments;
    return function() {
        runSequence.apply(this, args);
    }
}

// Run browserSync that proxies to Artifactory REST Server
// gulp.task("serve:dev", ["build:dev", "watch:dev"], connectTo('http://10.100.1.110:8081'));

gulp.task("serve", (callback) => {
    webpackConfig.watch = true;
    runSequence(
            "build:dev",
            "watch:dev",
            'browserSync',
            callback
    );
});

gulp.task("browserSync", (callback) => {
    'use strict';

    let url = 'http://localhost:8080';
    let cookie = undefined;
    browserSync({
        server: CONFIG.DESTINATIONS.TARGET,
        files: CONFIG.DESTINATIONS.TARGET,
        port: 8000,
        injectChanges: true,
        ghostMode: false,
        open: true,
        middleware: [
            proxy('/ui', {
                target: url + '/artifactory',
//                logLevel: 'debug',
                onProxyRes: proxyRes => {
                    if(proxyRes.headers['set-cookie']) {
                        cookie = proxyRes.headers['set-cookie'];
                    }
                },
                onProxyReq: proxyReq => {
                    if (cookie) proxyReq.setHeader('cookie', cookie);
                },
                headers: {cookie: cookie || ''}
            }),
            proxy('/', {
                target: url+'/artifactory/webapp'
            })
        ]
    });
    callback();
});

// Set watchers and run relevant tasks - then reload (when running under browsersync)
gulp.task('watch', function () {
    gulp.watch('./bower.json', sequence('bower', ['vendorScripts', 'vendorStyles', 'vendorStylesAssets', 'vendorFonts'], 'reload'));
    // gulp.watch(CONFIG.SOURCES.APPLICATION_JS, sequence('webpack', 'reload'));
    gulp.watch(CONFIG.SOURCES.TEMPLATES, sequence('templates', 'reload'));
    gulp.watch(CONFIG.SOURCES.REQUIRED_TEMPLATES, sequence('webpack', 'reload'));
    gulp.watch(CONFIG.SOURCES.LESS, sequence('less', 'reloadCss'));
    gulp.watch(CONFIG.SOURCES.VENDOR_JS, sequence(['vendorScripts', 'vendorStyles', 'vendorStylesAssets', 'vendorFonts'], 'reload'));
    gulp.watch(CONFIG.SOURCES.VENDOR_CSS, sequence(['vendorStyles'], 'reloadCss'));
    gulp.watch(CONFIG.SOURCES.FONTS, sequence('fonts', 'reload'));
    gulp.watch(CONFIG.SOURCES.INDEX, sequence('copyHtml', 'reload'));
});


gulp.task('watch-essentials', function (cb) {
    fs.writeFile('./components/jfrog-ui-essentials/.announceBuildCompletion', '*', () => {
        gulp.watch(CONFIG.SOURCES.TEMPLATES, sequence('templates', 'reload'));
        gulp.watch(CONFIG.SOURCES.LESS, sequence('less', 'reloadCss'));
        gulp.watch('./components/jfrog-ui-essentials/.announceBuildCompletion', sequence('vendorScripts', 'reload'));
        cb();
    })
});
gulp.task('serve-essentials', function (callback) {
    webpackConfig.watch = true;
    runSequence(
            "build:dev",
            "watch-essentials",
            'browserSync',
            callback
    );
});

// Watch the styleguide
gulp.task('watch:dev', ["watch"], function () {
    gulp.watch(CONFIG.SOURCES.STYLEGUIDE, sequence('copyStyleguide', 'reload'));
});

// install bower dependedencies
gulp.task('bower', function () {
    return gulp.src(['./bower.json'])
        .pipe(install());
});

// bundle application code
gulp.task('webpack', (callback) => {
    'use strict';

    let oneTimeCallback = callback;
    return webpack(webpackConfig, (error, stats) => {
        if (error) {
            gutil.log('[webpack]', error);
        }

        gutil.log('[webpack]', stats.toString({
            colors      : gutil.colors.supportsColor,
            hash        : false,
            timings     : false,
            chunks      : false,
            chunkModules: false,
            modules     : false,
            children    : true,
            version     : true,
            cached      : false,
            cachedAssets: false,
            reasons     : false,
            source      : false,
            errorDetails: false
        }));

        if (oneTimeCallback) {
            oneTimeCallback();
            oneTimeCallback = null;
        }
    });
});

// cache templates
gulp.task('templates', function () {
    return gulp.src(CONFIG.SOURCES.TEMPLATES)
        .pipe(html2js({
            outputModuleName: 'artifactory.templates',
            base: 'app/',
            useStrict: true
        }))
        .pipe(concat('templates.js'))
        .pipe(uglify({mangle:false}))
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET))
});

// concat vendor scripts
gulp.task('vendorScripts', function () {
    return gulp.src(CONFIG.SOURCES.VENDOR_SCRIPTS)
        .pipe(concat('vendorScripts.js'))
        .pipe(uglify({mangle:false}))
        .pipe(replace('<<div class=\'jstree-li', '<div class=\'jstree-li'))
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET));
});

// concat vendor stylesheets
gulp.task('vendorStyles', function () {
    return gulp.src(CONFIG.SOURCES.VENDOR_CSS)
        .pipe(concat('vendorStyles.css'))
        .pipe(minifyCss())
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET + '/css'));
});

// copy vendor assets to css
gulp.task('vendorStylesAssets', function () {
    return gulp.src(CONFIG.SOURCES.VENDOR_ASSETS)
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET + '/css'));
});

gulp.task('vendorFonts', function () {
    return gulp.src(CONFIG.SOURCES.VENDOR_FONTS)
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET + '/fonts'));
});

gulp.task('iconfonts', function(){
    return gulp.src(CONFIG.SOURCES.MEDIUM_SVG_ICONS)
        .pipe(iconfontCss({
            fontName: 'medium_svgicons'
        }))
        .pipe(iconfont({
            fontName: 'medium_svgicons'
        }))
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET + '/css'));
});


// compile less
gulp.task('less', function () {
    var combined = combiner.obj([
        gulp.src(CONFIG.SOURCES.LESS_MAIN_FILE),
//        sourceMaps.init(),
        less({paths: [path.join(__dirname, 'less', 'includes')]}),
        prefixer(),
        concat('application.css'),
//        sourceMaps.write(),
        minifyCss(),
        gulp.dest(CONFIG.DESTINATIONS.TARGET + '/css')
    ]);

    // any errors in the above streams will get caught
    // by this listener, instead of being thrown:
    combined.on('error', console.error.bind(console));

    return combined;
});

// copy html file to dest
gulp.task('copyHtml', function () {
    return gulp.src(CONFIG.SOURCES.INDEX)
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET))
});

// copy styleguide file to dest - for development only
gulp.task('copyStyleguide', function () {
    return gulp.src(CONFIG.SOURCES.STYLEGUIDE)
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET))
});

//copy fonts
gulp.task('fonts', function () {
    return gulp.src(CONFIG.SOURCES.FONTS)
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET + '/fonts'))
});

//copy images
gulp.task('images', function () {
    return gulp.src(CONFIG.SOURCES.IMAGES + '/**/*')
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET + '/images'))
});

//copy jquery-ui images
gulp.task('jqueryui-images', function () {
    return gulp.src(CONFIG.SOURCES.JQUERY_UI_IMAGES)
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET + '/css/images'))
});

//copy jfrog-ui-essentials images
gulp.task('jfrog-ui-images', function () {
    return gulp.src(CONFIG.SOURCES.JFROG_UI_IMAGES)
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET + '/css/images'))
});

gulp.task('jfrog-ui-fonts', function () {
    return gulp.src(CONFIG.SOURCES.JFROG_UI_FONTS)
        .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET + '/css/fonts'))
});

function transformFilename(file, hash) {
    var ext = path.extname(file.path);
    return path.basename(file.path, ext) + '.' + process.env.BUILD_NUMBER + ext; // filename.<BUILD_NUMBER>.ext
}

gulp.task("revision", function(){
    if (process.env.BUILD_NUMBER) {
        var revAll = new RevAll({transformFilename: transformFilename});
        return gulp.src(CONFIG.DESTINATIONS.TARGET_REV)
            .pipe(revAll.revision())
            .pipe(revNapkin({force:true}))
            .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET))
            .pipe(revAll.manifestFile())
            //     .pipe(revDel({ dest: CONFIG.DESTINATIONS.TARGET, force: true }))
            .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET))
    }
})

gulp.task("revreplace", ['revision'], function() {
    if (process.env.BUILD_NUMBER) {
        var manifest = gulp.src(CONFIG.DESTINATIONS.TARGET + "/rev-manifest.json");
        return gulp.src(CONFIG.SOURCES.INDEX)
            .pipe(revReplace({manifest: manifest}))
            .pipe(gulp.dest(CONFIG.DESTINATIONS.TARGET))
    }
});


gulp.task('karma', function (done) {
    karma.server.start({
        configFile: __dirname + '/karma.conf.js',
        singleRun: true
    }, done);
});
