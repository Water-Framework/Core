
/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.water.core.api.repository.query;

import java.util.List;

/**
 *
 * @Author Aristide Cittadino.
 * Interface which maps the concept of operation inside an expression.
 * An operation can be unitary or binary or more and have multiple operands.
 */
public interface QueryFilterOperation {
	/**
	 *
	 * @return Operation Name
	 */
	String getName();
	/**
	 *
	 * @return Operand string rapresentation
	 */
	String operator();
	/**
	 *
	 * @return Number of operands
	 */
	int numOperands();
	/**
	 *
	 * @return true if it needs an expression or a value
	 */
	boolean needsExpr();

	/**
	 * and condition with another query
	 * @param rightQuery
	 * @return
	 */
	Query and(Query rightQuery);

	/**
	 * Or condition with another query
	 * @param rightQuery
	 * @return
	 */
	Query or(Query rightQuery);

	/**
	 * Not condition of the current operation
	 * @return
	 */
	Query not();

	/**
	 * In condition with a list of values
	 * @param values
	 * @return
	 */
	Query in(List<?> values);
}
