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

import groovy.transform.ToString

import org.spockframework.runtime.AbstractRunListener
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo

class ScoreKeeper extends AbstractRunListener {
	enum Result {
		PASSED,
		FAILED,
		SKIPPED
	}
	
	@ToString
	static class Stats {
		final IterationInfo itrInfo
		final Date start
		long elapsedNanos
		Result result
		Throwable error
		
		private boolean stopped = false
		private startTs
		
		Stats(IterationInfo info) {
			itrInfo = info
			result = Result.PASSED // assume passed until told otherwise
			start = new Date()
			startTs = System.nanoTime()	
		}
		
		private void stop() {
			if(!stopped) {
				this.@elapsedNanos = System.nanoTime() - startTs
				this.@stopped = true
			} else
				throw new IllegalStateException('Cannot stop a feature more than once!')
		}
		
		void setElapsedNanos(long n) { throw new UnsupportedOperationException('Read only') }
		void setResult(Result r) { throw new UnsupportedOperationException('Read only') }
		void setError(Throwable t) { throw new UnsupportedOperationException('Read only') }
	}
	
	private FeatureInfo currentFeature
	private IterationInfo currentItr
	Map stats = [:]
	
	Date getStartDate() { 
		stats.collect { it.value.min { it.start }.start }.min()
	}
	
	@Override
	void beforeIteration(IterationInfo info) {
		currentItr = info
		stats[currentFeature] << new Stats(info)
	}
	
	@Override
	void beforeFeature(FeatureInfo info) {
		currentFeature = info
		stats[info] = []
	}
	
	@Override
	void afterIteration(IterationInfo info) {
		stats[currentFeature][-1].stop()
		currentItr = null
	}
	
	@Override
	void afterFeature(FeatureInfo info) {
		currentFeature = null
	}
	
	@Override
	void error(ErrorInfo info) {
		def stats = this.stats[currentFeature][-1]
		stats.@result = Result.FAILED
		stats.@error = info.exception
		
	}
	
	@Override
	void featureSkipped(FeatureInfo info) {
		stats[currentFeature] = []
	}
}
