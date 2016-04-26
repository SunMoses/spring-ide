/*******************************************************************************
 * Copyright (c) 2016 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.dash.cloudfoundry.client.v2;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.spring.client.SpringCloudFoundryClient;

/**
 * TODO: Remove this class when the 'thread leak bug' in V2 client is fixed.
 *
 * At the moment each time {@link SpringCloudFoundryClient} is create a threadpool
 * is created by the client and it is never cleaned up. The only way we have
 * to mitigate this leak is to try and create as few clients as possible.
 * <p>
 * So we have a permanent cache of clients here that is reused.
 * <p>
 * When the bug is fixed then this should no longer be necessary and we can removed this cache
 * and just create the client as needed.
 *
 * @author Kris De Volder
 */
public class CloudFoundryClientCache {

	private static final boolean DEBUG = true;

	private static void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}


	public static class Params {
		public final String username;
		public final String password;
		public final String host;
		public Params(String username, String password, String host) {
			super();
			this.username = username;
			this.password = password;
			this.host = host;
		}
		@Override
		public String toString() {
			return "Params [username=" + username + ", host=" + host + "]";
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((host == null) ? 0 : host.hashCode());
			result = prime * result + ((password == null) ? 0 : password.hashCode());
			result = prime * result + ((username == null) ? 0 : username.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Params other = (Params) obj;
			if (host == null) {
				if (other.host != null)
					return false;
			} else if (!host.equals(other.host))
				return false;
			if (password == null) {
				if (other.password != null)
					return false;
			} else if (!password.equals(other.password))
				return false;
			if (username == null) {
				if (other.username != null)
					return false;
			} else if (!username.equals(other.username))
				return false;
			return true;
		}

	}

	private Map<Params, CloudFoundryClient> cache = new HashMap<>();

	private int clientCount = 0;

	public synchronized CloudFoundryClient getOrCreate(String username, String password, String host) {
		Params params = new Params(username, password, host);
		CloudFoundryClient client = cache.get(params);
		if (client==null) {
			clientCount++;
			debug("Creating client ["+clientCount+"]: "+params);
			cache.put(params, client = create(params));
		} else {
			debug("Reusing client ["+clientCount+"]: "+params);
		}
		return client;
	}

	protected CloudFoundryClient create(Params params) {
		return SpringCloudFoundryClient.builder()
				.username(params.username)
				.password(params.password)
				.host(params.host)
				.build();
	}

}
