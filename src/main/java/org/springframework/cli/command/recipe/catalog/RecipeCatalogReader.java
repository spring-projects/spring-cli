/*
 * Copyright 2021 - 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cli.command.recipe.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Fabian Krüger
 */
@Component
class RecipeCatalogReader {
    public RecipeCatalogEntries loadCatalogData(InputStream recipeCatalogInputStream) {
        try {
            ObjectMapper om  = new ObjectMapper(new YAMLFactory());
            RecipeCatalogEntries recipeCatalogEntries = om.readValue(recipeCatalogInputStream, RecipeCatalogEntries.class);
            return recipeCatalogEntries;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
