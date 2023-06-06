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

package org.apache.couchdb.nouveau;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Environment;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import java.util.concurrent.ForkJoinPool;
import org.apache.couchdb.nouveau.core.IndexManager;
import org.apache.couchdb.nouveau.core.UpdatesOutOfOrderExceptionMapper;
import org.apache.couchdb.nouveau.health.AnalyzeHealthCheck;
import org.apache.couchdb.nouveau.health.IndexHealthCheck;
import org.apache.couchdb.nouveau.lucene9.Lucene9Module;
import org.apache.couchdb.nouveau.lucene9.ParallelSearcherFactory;
import org.apache.couchdb.nouveau.resources.AnalyzeResource;
import org.apache.couchdb.nouveau.resources.IndexResource;
import org.apache.couchdb.nouveau.tasks.CloseAllIndexesTask;
import org.apache.lucene.search.SearcherFactory;

public class NouveauApplication extends Application<NouveauApplicationConfiguration> {

    public static void main(String[] args) throws Exception {
        new NouveauApplication().run(args);
    }

    @Override
    public String getName() {
        return "Nouveau";
    }

    @Override
    public void run(NouveauApplicationConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(new UpdatesOutOfOrderExceptionMapper());

        // configure index manager
        final IndexManager indexManager = new IndexManager();
        indexManager.setCommitIntervalSeconds(configuration.getCommitIntervalSeconds());
        indexManager.setIdleSeconds(configuration.getIdleSeconds());
        indexManager.setMaxIndexesOpen(configuration.getMaxIndexesOpen());
        indexManager.setMetricRegistry(environment.metrics());
        indexManager.setScheduler(environment
                .lifecycle()
                .scheduledExecutorService("index-manager-%d")
                .threads(5)
                .build());
        indexManager.setObjectMapper(environment.getObjectMapper());
        indexManager.setRootDir(configuration.getRootDir());
        environment.lifecycle().manage(indexManager);

        // Serialization classes
        environment.getObjectMapper().registerModule(new Lucene9Module());

        // AnalyzeResource
        final AnalyzeResource analyzeResource = new AnalyzeResource();
        environment.jersey().register(analyzeResource);

        // IndexResource
        final SearcherFactory searcherFactory = new ParallelSearcherFactory(ForkJoinPool.commonPool());
        final IndexResource indexResource = new IndexResource(indexManager, searcherFactory);
        environment.jersey().register(indexResource);

        // Health checks
        environment.healthChecks().register("analyze", new AnalyzeHealthCheck(analyzeResource));
        environment.healthChecks().register("index", new IndexHealthCheck(indexResource));

        // configure tasks
        environment.admin().addTask(new CloseAllIndexesTask(indexManager));

        // Swagger
        environment.jersey().register(new OpenApiResource());
    }
}
