//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.couchdb.nouveau.api;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.jackson.JsonSnakeCase;

@JsonSnakeCase
public class AnalyzeRequest {

    @NotNull
    private LuceneVersion luceneVersion;

    @NotEmpty
    private String analyzer;

    @NotEmpty
    private String text;

    public AnalyzeRequest() {
        // Jackson deserialization
    }

    public AnalyzeRequest(final LuceneVersion luceneVersion, final String analyzer, final String text) {
        this.luceneVersion = luceneVersion;
        this.analyzer = analyzer;
        this.text = text;
    }

    @JsonProperty
    public LuceneVersion getLuceneVersion() {
        return luceneVersion;
    }

    @JsonProperty
    public String getAnalyzer() {
        return analyzer;
    }

    @JsonProperty
    public String getText() {
        return text;
    }

}
