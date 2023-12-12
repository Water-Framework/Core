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
package it.water.core.validation.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public abstract class NoMalitiusCodeAbstractValidator {
    private Logger log = LoggerFactory.getLogger(NoMalitiusCodeAbstractValidator.class.getName());
    public static final String SQL_TYPES =
            "TABLE, TABLESPACE, PROCEDURE, FUNCTION, TRIGGER, KEY, VIEW, MATERIALIZED VIEW, LIBRARY" +
                    "DATABASE LINK, DBLINK, INDEX, CONSTRAINT, TRIGGER, USER, SCHEMA, DATABASE, PLUGGABLE DATABASE, BUCKET, " +
                    "CLUSTER, COMMENT, SYNONYM, TYPE, JAVA, SESSION, ROLE, PACKAGE, PACKAGE BODY, OPERATOR" +
                    "SEQUENCE, RESTORE POINT, PFILE, CLASS, CURSOR, OBJECT, RULE, USER, DATASET, DATASTORE, " +
                    "COLUMN, FIELD, OPERATOR";

    public static final String SQL_CODE_REGEX = ")(\\b)+\\s.*(.*)";
    private static Pattern[] scriptPatterns = new Pattern[]{
            // Script fragments
            Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
            // src='...'
            Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // lonely script tags
            Pattern.compile(".*</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*<script(.*?)>",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // eval(...)
            Pattern.compile(".*eval\\((.*?)\\)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // expression(...)
            Pattern.compile(".*expression\\((.*?)\\)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // javascript:...
            Pattern.compile(".*javascript:.*", Pattern.CASE_INSENSITIVE),
            // vbscript:...
            Pattern.compile(".*vbscript:.*", Pattern.CASE_INSENSITIVE),
            // onload(...)=...
            Pattern.compile(".*onload(.*?)=",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile(".*onError(.*?)=(.*?)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
    };

    private static Pattern[] sqlPatterns = new Pattern[]{
            Pattern.compile("(?i)(.*)(\\b)SELECT(\\b)\\s.*(\\b)FROM(\\b)\\s.*(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)INSERT(\\b)\\s.*(\\b)INTO(\\b)\\s.*(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)UPDATE(\\b)\\s.*(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)DELETE(\\b)\\s.*(\\b)FROM(\\b)\\s.*(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)UPSERT(\\b)\\s.*(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)SAVEPOINT(\\b)\\s.*(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)CALL(\\b)\\s.*(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)ROLLBACK(\\b)\\s.*(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)KILL(\\b)\\s.*(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)DROP(\\b)\\s.*(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)CREATE(\\b)(\\s)*(" + SQL_TYPES.replace(",", "|") + SQL_CODE_REGEX, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)ALTER(\\b)(\\s)*(" + SQL_TYPES.replace(",", "|") + SQL_CODE_REGEX, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)TRUNCATE(\\b)(\\s)*(" + SQL_TYPES.replace(",", "|") + SQL_CODE_REGEX, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)LOCK(\\b)(\\s)*(" + SQL_TYPES.replace(",", "|") + SQL_CODE_REGEX, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)UNLOCK(\\b)(\\s)*(" + SQL_TYPES.replace(",", "|") + SQL_CODE_REGEX, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)RELEASE(\\b)(\\s)*(" + SQL_TYPES.replace(",", "|") + SQL_CODE_REGEX, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)DESC(\\b)(\\w)*\\s.*(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(?i)(.*)(\\b)DESCRIBE(\\b)(\\w)*\\s.*(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(.*)(/\\*|\\*/|;)+(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("(.*)(-){2,}(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
    };

    protected boolean validate(String value, Runnable defineViolations) {
        log.debug("Validating value with @NoMalitiusCode...");
        if (value == null)
            return true;
        List<Pattern> patterns = new ArrayList<>();
        patterns.addAll(Arrays.asList(scriptPatterns));
        patterns.addAll(Arrays.asList(sqlPatterns));
        for (Pattern scriptPattern : patterns) {
            if (scriptPattern.matcher(value).matches()) {
                defineViolations.run();
                return false;
            }
        }
        return true;
    }

}
