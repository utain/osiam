/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2013-2016 tarent solutions GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.osiam.storage.query;

import org.osiam.resources.exception.InvalidFilterException;
import org.osiam.resources.scim.User;
import org.osiam.storage.entities.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class UserFilterExpression extends FilterExpression<UserEntity> {

    private Field field;

    UserFilterExpression(String field, FilterConstraint constraint, String value) {
        super(constraint, value);
        this.field = new Field(field);
    }

    @Override
    public UserFilterExpression.Field getField() {
        return field;
    }

    public static class Field implements FilterExpression.Field<UserEntity> {
        private static final Logger logger = LoggerFactory.getLogger(UserFilterExpression.Field.class);
        private String urn;
        private String name;
        private UserQueryField queryField;

        public Field(String field) {
            if (field.toLowerCase().startsWith(User.SCHEMA.toLowerCase())) {
                if (field.charAt(User.SCHEMA.length()) == '.') {
                    throw new InvalidFilterException(String.format("Period (.) is not a valid field separator: %s", field));
                }
                name = field.substring(User.SCHEMA.length() + 1).toLowerCase(Locale.ENGLISH);
            } else {
                name = field.toLowerCase(Locale.ENGLISH);
            }

            // Try to resolve a core schema field.
            queryField = UserQueryField.fromString(name);

            urn = User.SCHEMA;
            // we can't recognize a core schema field, it's an extension.
            if (queryField == null) {

                // lastIndexOf returns -1 if char can not be found in the string
                int lastIndexOfPeriod = field.lastIndexOf('.');
                int lastIndexOfColon = field.lastIndexOf(':');

                // the last string part of the string is separated by a period, that is not SCIM conform but has been
                // supported by OSIAM for a long time so we generate a warning. As soon as we support extended attributes
                // for extensions this needs to go.
                if (lastIndexOfPeriod > lastIndexOfColon) {
                    urn = field.substring(0, lastIndexOfPeriod);
                    name = field.substring(lastIndexOfPeriod + 1);
                    logger.warn(
                            String.format("Period (.) used as field separator in %s. This is not SCIM conform and deprecated!",
                                    field));
                } else if (lastIndexOfColon > lastIndexOfPeriod) {
                    // a colon is separating the last part of this string and the field of the extension from it's URN.
                    // Just as it's supposed to be.
                    urn = field.substring(0, lastIndexOfColon);
                    name = field.substring(lastIndexOfColon + 1);
                } else {
                    // Neither colon nor period found in string. It's a user error.
                    throw new InvalidFilterException(String.format("Unable to parse field or extension from %s",
                            field));
                }
            }
        }

        @Override
        public String getUrn() {
            return urn;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public UserQueryField getQueryField() {
            return queryField;
        }

        @Override
        public boolean isExtension() {
            return queryField == null;
        }
    }
}
