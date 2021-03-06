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

package org.artifactory.storage.db.aql.parser.elements.high.level.domain.sensitive.dynamic;

import org.artifactory.storage.db.aql.parser.AqlParser;
import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.high.level.domain.sensitive.DomainSensitiveParserElement;

/**
 * @author Gidi Shabat
 */
public class DynamicStar extends DomainSensitiveParserElement {

    @Override
    protected ParserElement init() {
        switch (domain) {
            case items: {
                return AqlParser.itemStar;
            }
            case archives: {
                return AqlParser.archiveStar;
            }
            case entries: {
                return AqlParser.entriesStar;
            }
            case properties: {
                return AqlParser.propertiesStar;
            }
            case statistics: {
                return AqlParser.statisticsStar;
            }
            case artifacts: {
                return AqlParser.buildArtifactStar;
            }
            case dependencies: {
                return AqlParser.buildDependenciesStar;
            }
            case builds: {
                return AqlParser.buildStar;
            }
            case modules: {
                return AqlParser.buildModuleStar;
            }
            case moduleProperties: {
                return AqlParser.buildModulePropertiesStar;
            }
            case buildPromotions: {
                return AqlParser.buildPromotionsStar;
            }
            case buildProperties: {
                return AqlParser.buildPropertiesStar;
            }
        }
        throw new UnsupportedOperationException("Unsupported domain :" + domain);
    }
}