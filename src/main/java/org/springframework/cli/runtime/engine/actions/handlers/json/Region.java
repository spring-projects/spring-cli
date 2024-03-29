/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.cli.runtime.engine.actions.handlers.json;

/**
 * Trivial implementation of {@link IRegion}
 *
 * @author kdvolder
 *
 */
public class Region implements IRegion {

	private final int ofs;

	private final int len;

	public Region(int ofs, int len) {
		super();
		this.ofs = ofs;
		this.len = len;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Region other = (Region) obj;
		if (len != other.len) {
			return false;
		}
		if (ofs != other.ofs) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + len;
		result = prime * result + ofs;
		return result;
	}

	@Override
	public String toString() {
		return "Region [ofs=" + ofs + ", len=" + len + "]";
	}

	@Override
	public int getOffset() {
		return ofs;
	}

	@Override
	public int getLength() {
		return len;
	}

}
