/*
 *             Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.model.util;

import com.bluelinelabs.logansquare.typeconverters.TypeConverter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by mariotaku on 2017/3/25.
 */

public class UnixEpochSecondDateConverter implements TypeConverter<Date> {
    @Override
    public Date parse(final JsonParser jsonParser) throws IOException {
        long value = jsonParser.nextLongValue(-1);
        return new Date(TimeUnit.MILLISECONDS.toSeconds(value));
    }

    @Override
    public void serialize(final Date object, final String fieldName,
            final boolean writeFieldNameForObject, final JsonGenerator jsonGenerator) throws IOException {
        if (writeFieldNameForObject) {
            jsonGenerator.writeFieldName(fieldName);
        }
        if (object == null) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeNumber(TimeUnit.SECONDS.toMillis(object.getTime()));
        }
    }
}
