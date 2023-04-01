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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;

import org.apache.couchdb.nouveau.core.IndexManager;
import org.apache.couchdb.nouveau.core.UpdatesOutOfOrderExceptionMapper;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;

public class NouveauApplication extends Application<NouveauApplicationConfiguration> {

    private IndexManager indexManager;

    public static void main(String[] args) throws Exception {
        new NouveauApplication().run(args);
    }

    @Override
    public String getName() {
        return "Nouveau";
    }

    @Override
    public void initialize(Bootstrap<NouveauApplicationConfiguration> bootstrap) {
        indexManager = new IndexManager();

        // Find Lucene bundles
        for (String name : System.getProperties().stringPropertyNames()) {
            if (name.startsWith("nouveau.bundle.")) {
                try {
                    ClassLoader classLoader = URLClassLoader
                            .newInstance(new URL[] { new URL(System.getProperty(name)) });
                    final ServiceLoader<LuceneBundle> bundleLoader = ServiceLoader.load(LuceneBundle.class,
                            classLoader);
                    for (final LuceneBundle bundle : bundleLoader) {
                        bundle.setIndexManager(indexManager);
                        bootstrap.addBundle(bundle);
                    }
                } catch (final MalformedURLException e) {
                    throw new Error(e);
                }
            }
        }
    }

    @Override
    public void run(NouveauApplicationConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(new UpdatesOutOfOrderExceptionMapper());

        // configure index manager
        indexManager.setCommitIntervalSeconds(configuration.getCommitIntervalSeconds());
        indexManager.setIdleSeconds(configuration.getIdleSeconds());
        indexManager.setLockCount(configuration.getLockCount());
        indexManager.setMaxIndexesOpen(configuration.getMaxIndexesOpen());
        indexManager.setMetricRegistry(environment.metrics());
        indexManager.setScheduler(environment.lifecycle().scheduledExecutorService("index-manager-%d").threads(5).build());
        indexManager.setObjectMapper(environment.getObjectMapper());
        indexManager.setRootDir(configuration.getRootDir());
        environment.lifecycle().manage(indexManager);
    }

}
