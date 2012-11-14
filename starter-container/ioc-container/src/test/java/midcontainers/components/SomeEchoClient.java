/*
 * Copyright (C) 2011 Julien Ponge, Institut National des Sciences Appliquées de Lyon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package midcontainers.components;

import midcontainers.Named;

public class SomeEchoClient implements EchoClient {
    private final Echo echo;
    private final String message;

    public SomeEchoClient(Echo echo, @Named("message") String message) {
        this.echo = echo;
        this.message = message;
    }

    @Override
    public String run() {
        return echo.echo(message);
    }
}