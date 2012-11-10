/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.ivyresolve;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.gradle.api.internal.artifacts.ivyservice.BuildableArtifactResolveResult;
import org.gradle.api.internal.artifacts.ivyservice.ModuleVersionResolveException;

public class LocalModuleVersionRepository implements LocalAwareModuleVersionRepository {
    private final ModuleVersionRepository delegate;

    public LocalModuleVersionRepository(ModuleVersionRepository delegate) {
        this.delegate = delegate;
    }

    public boolean isLocal() {
        return delegate.isLocal();
    }

    public void download(Artifact artifact, BuildableArtifactResolveResult result) {
        delegate.download(artifact, result);
    }

    public ModuleVersionDescriptor getLocalDependency(DependencyDescriptor dd) throws ModuleVersionResolveException {
        throw new UnsupportedOperationException();
    }

    public ModuleVersionDescriptor getDependency(DependencyDescriptor dd) throws ModuleVersionResolveException {
        return delegate.getDependency(dd);
    }

    public String getId() {
        return delegate.getId();
    }

    public String getName() {
        return delegate.getName();
    }
}
