/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.mobileappstudy.surveydesign;

/**
 * Created by iansigmon on 2/2/17.
 */

/**
 * Exception thrown if survey design metadata is invalid
 *
 * note: runtime exception used due to method references and lambda expressions
 */
public class InvalidDesignException extends RuntimeException
{
    public InvalidDesignException(String message)
    {
        this(message, null);
    }

    public InvalidDesignException(String message, Throwable inner)
    {
        super(message, inner);
    }
}
