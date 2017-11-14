/* Copyright (c) 2017 Marcin Zajączkowski
 * All rights reserved.
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
package info.solidsoft.gradle.pitest

import spock.lang.Issue

class PitestPluginClasspathFilteringSpec extends BasicProjectBuilderSpec {

    private PitestTask task

    @Issue('https://github.com/szpak/gradle-pitest-plugin/issues/52')
    def "should filter dynamic library '#libFileName' by default"() {
        given:
            File libFile = new File(tmpProjectDir.root, libFileName)
        and:
            task = getJustOnePitestTaskOrFail()
        expect:
            !task.createTaskArgumentMap()['classPath'].contains(libFile.path)
        where:
            libFileName << ['lib.so', 'win.dll', 'dyn.dylib']   //TODO: Add test with more than one element
    }

    def "should filter .pom file by default"() {
        given:
            File pomFile = addFileWithFileNameAsCompileDependencyAndReturnAsFile('foo.pom')
        and:
            task = getJustOnePitestTaskOrFail()
        expect:
            !task.createTaskArgumentMap()['classPath'].contains(pomFile.path)
    }

    def "should not filer regular dependency '#depFileName' by default"() {
        given:
            File depFile = addFileWithFileNameAsCompileDependencyAndReturnAsFile(depFileName)
        and:
            task = getJustOnePitestTaskOrFail()
        expect:
            task.createTaskArgumentMap()['classPath'].contains(depFile.path)
        where:
            depFileName << ['foo.jar', 'foo.zip']
    }

    def "should not filer source set directory by default"() {
        given:
            File testClassesDir = new File(new File(new File(new File(tmpProjectDir.root, 'build'), 'classes'), 'java'), 'test')
        and:
            task = getJustOnePitestTaskOrFail()
        expect:
            task.createTaskArgumentMap()['classPath'].contains(testClassesDir.path)
    }

    def "should filter excluded dependencies remaining regular ones"() {
        given:
            File depFile = addFileWithFileNameAsCompileDependencyAndReturnAsFile('foo.jar')
        and:
            File libDepFile = addFileWithFileNameAsCompileDependencyAndReturnAsFile('bar.so')
        and:
            task = getJustOnePitestTaskOrFail()
        expect:
            task.createTaskArgumentMap()['classPath'].contains(depFile.path)
            !task.createTaskArgumentMap()['classPath'].contains(libDepFile.path)
    }

    def "should filter user defined extensions"() {
        given:
            File depFile = addFileWithFileNameAsCompileDependencyAndReturnAsFile('file.extra')
        and:
            project.pitest.fileExtensionsToFilter = ['extra']
        and:
            task = getJustOnePitestTaskOrFail()
        expect:
            !task.createTaskArgumentMap()['classPath'].contains(depFile.path)
    }

    def "should allow to override extensions filtered by default"() {
        given:
            File depFile = addFileWithFileNameAsCompileDependencyAndReturnAsFile('needed.so')
        and:
            project.pitest.fileExtensionsToFilter = ['extra']
        and:
            task = getJustOnePitestTaskOrFail()
        expect:
            task.createTaskArgumentMap()['classPath'].contains(depFile.path)
    }

    def "should allow to provide extra extensions in addition to default ones"() {
        given:
            File libDepFile = addFileWithFileNameAsCompileDependencyAndReturnAsFile('default.so')
            File extraDepFile = addFileWithFileNameAsCompileDependencyAndReturnAsFile('file.extra')
        and:
            project.pitest.fileExtensionsToFilter += ['extra']
        and:
            task = getJustOnePitestTaskOrFail()
        expect:
            String resolvedPitClasspath = task.createTaskArgumentMap()['classPath']
            !resolvedPitClasspath.contains(libDepFile.path)
            !resolvedPitClasspath.contains(extraDepFile.path)
    }

    def "should not fail on fileExtensionsToFilter set to null"() {
        given:
            project.pitest.fileExtensionsToFilter = null
        and:
            task = getJustOnePitestTaskOrFail()
        when:
            String resolvedPitClasspath = forceClasspathResolutionInPluginAndReturnIt()
        then:
            noExceptionThrown()
        and:
            resolvedPitClasspath.contains('main')
    }

    private String forceClasspathResolutionInPluginAndReturnIt() {
        return task.createTaskArgumentMap()['classPath']
    }

    private File addFileWithFileNameAsCompileDependencyAndReturnAsFile(String depFileName) {
        File depFile = new File(tmpProjectDir.root, depFileName)
        project.dependencies.add('compile', project.files(depFile))
        return depFile
    }
}
