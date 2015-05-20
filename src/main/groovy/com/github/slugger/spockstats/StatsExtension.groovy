/*
 *      Copyright 2015 Battams, Derek
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 */
package com.github.slugger.spockstats

import groovy.json.JsonOutput

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.SpecInfo

import com.github.slugger.spockstats.ScoreKeeper.Result

class StatsExtension implements IGlobalExtension {
	private Map specs = [:]
	
	StatsExtension() {
		Runtime.runtime.addShutdownHook {
			def json = [:]
			json.product = 'SIQ4L'
			json.version = '1.0'
			json.build = '1233'
			json.specs = specs.collect { specInfo, scoreKeeper ->
				def data = [
					specName: specInfo.description.toString(),
					start: scoreKeeper.startDate.time,
					features: scoreKeeper.stats.collect { featureInfo, stats ->
						def fData = [
							name: featureInfo.name,
							result: stats.findResult { it.result == Result.FAILED ? it.result : null } ?: Result.PASSED,
							exeTime: stats.sum { it.elapsedNanos },
							iterations: stats.collect {
								def os = null
								if(it.error) {
									os = new ByteArrayOutputStream()
									os.withStream { out ->
										it.error.printStackTrace(new PrintStream(out))
									}
								}
								[
									name: it.itrInfo.name,
									result: it.result,
									start: it.start.time,
									exeTime: it.elapsedNanos,
									error: os ? os.toString() : null
								]
							} 
						]
						fData.totals = [
							passed: fData.iterations.findAll { it.result == Result.PASSED }.size(),
							failed: fData.iterations.findAll { it.result == Result.FAILED }.size(),
							skipped: fData.iterations.findAll { it.result == Result.SKIPPED }.size()
						]
						fData.totals.total = fData.totals.values().sum()
						fData.passPercentage = fData.totals.passed / (fData.totals.total - fData.totals.skipped) * 100
						fData
					}
				]
				data.exeTime = data.features.sum { it.exeTime }
				data.totals = [
					passed: data.features.collect { it.totals.passed }.sum(),
					failed: data.features.collect { it.totals.failed }.sum(),
					skipped: data.features.collect { it.totals.skipped }.sum(),
					total: data.features.collect { it.totals.total}.sum()
				]
				data.passPercentage = data.totals.passed / (data.totals.total - data.totals.skipped) * 100
				data
			}
			json.exeTime = json.specs.sum { it.exeTime }
			json.start = json.specs.min { it.start }.start
			json.totals = [
				passed: json.specs.collect { it.totals.passed }.sum(),
				failed: json.specs.collect { it.totals.failed }.sum(),
				skipped: json.specs.collect { it.totals.skipped }.sum(),
				total: json.specs.collect { it.totals.total }.sum(),
			]
			json.passPercentage = json.totals.passed / (json.totals.total - json.totals.skipped) * 100
			def f = new File('spock-stats.json')
			if(f.exists())
				f.renameTo(new File("${f.name}.${new Date(f.lastModified()).format('yyyyMMddHHmmss')}"))
			f << JsonOutput.prettyPrint(JsonOutput.toJson(json))
		}
	}
	
	@Override
	void visitSpec(SpecInfo spec) {
		def listener = new ScoreKeeper()
		specs[spec] = listener
		spec.addListener(listener)
	}
}
