package com.pusher.client;

import com.pusher.client.channel.Channel;
import com.pusher.client.channel.ChannelEventListener;
import com.pusher.client.channel.PresenceChannel;
import com.pusher.client.channel.PresenceChannelEventListener;
import com.pusher.client.channel.PrivateChannel;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.channel.impl.ChannelManager;
import com.pusher.client.channel.impl.InternalChannel;
import com.pusher.client.channel.impl.PresenceChannelImpl;
import com.pusher.client.channel.impl.PrivateChannelImpl;
import com.pusher.client.connection.Connection;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.impl.InternalConnection;
import com.pusher.client.util.Factory;

/**
 * This class is the main entry point for accessing Pusher.
 * 
 * <p>By creating a new {@link Pusher} instance and calling {@link Pusher.connect()} a connection to Pusher is established.</p>
 *
 * <p>Subscriptions for data are represented by {@link com.pusher.client.channel.Channel} objects, or subclasses thereof. Subscriptions
 * are created by calling {@link #Pusher.subscribe(String)}, {@link #Pusher.subscribePrivate(String)}, {@link #Pusher.subscribePresence(String)} or
 * one of the overloads.</p>
 */
public class Pusher {

	private final PusherOptions pusherOptions;
	private final InternalConnection connection;
	private final ChannelManager channelManager;
	private final Factory factory;

	/**
	 * <p>
	 * Creates a new instance of Pusher.
	 * </p>
	 * 
	 * <p>
	 * Note that if you use this constructor you will not be able to subscribe to private or presence channels because no {@link Authorizer} has been set. If you want to use private or presence channels:
	 * <ul>
	 * <li>Create an implementation of the {@link Authorizer} interface, or use the {@link com.pusher.client.util.HttpAuthorizer} provided.</li>
	 * <li>Create an instance of {@link PusherOptions} and set the authorizer on it by calling {@link PusherOptions#setAuthorizer(Authorizer)}.</li>
	 * <li>Use the {@link #Pusher(String, PusherOptions)} constructor to create an instance of Pusher.</li>
	 * </ul>
	 * </p>
	 * 
	 * <p>
	 * The {@link com.pusher.client.example.PrivateChannelExampleApp} and {@link com.pusher.client.example.PresenceChannelExampleApp} example applications show how to do this.
	 * </p>
	 * 
	 * @param apiKey Your Pusher API key.
	 */
	public Pusher(String apiKey) {

		this(apiKey, new PusherOptions());
	}

	/**
	 * Creates a new instance of Pusher.
	 * 
	 * @param apiKey Your Pusher API key.
	 * @param pusherOptions Options for the Pusher client library to use.
	 */
	public Pusher(String apiKey, PusherOptions pusherOptions) {
		
		this(apiKey, pusherOptions, new Factory());
	}

	/**
	 * Creates a new Pusher instance using the provided Factory, 
	 * package level access for unit tests only.
	 */
	Pusher(String apiKey, PusherOptions pusherOptions, Factory factory) {

		if (apiKey == null || apiKey.length() == 0) {
			throw new IllegalArgumentException("API Key cannot be null or empty");
		}

		if (pusherOptions == null) {
			throw new IllegalArgumentException("PusherOptions cannot be null");
		}

		this.pusherOptions = pusherOptions;
		this.factory = factory;
		this.connection = factory.getConnection(apiKey, this.pusherOptions);
		this.channelManager = factory.getChannelManager();
		this.channelManager.setConnection(this.connection);
	}

	/* Connection methods */

	/**
	 * Gets the underlying {@link Connection} object that is being used by this instance of {@linkplain Pusher}.
	 * 
	 * @return The {@link Connection} object.
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * Connects to Pusher. Any {@link ConnectionEventListener}s that have already been registered using
	 * the {@link Connection#bind(ConnectionState, ConnectionEventListener)} method will receive connection events.
	 * 
	 * <p>Calls are ignored (a connection is not attempted) if the {@link Pusher.getConnection().getState()} is not {@link com.pusher.client.connection.ConnectionState.DISCONNECTED}.</p>
	 */
	public void connect() {
		connect(null);
	}

	/**
	 * Binds a {@link ConnectionEventListener} to the specified events and then connects to Pusher. This is equivalent to binding a {@link ConnectionEventListener} using the {@link Connection#bind(ConnectionState, ConnectionEventListener)} method before connecting.
	 * 
	 * <p>Calls are ignored (a connection is not attempted) if the {@link Pusher.getConnection().getState()} is not {@link com.pusher.client.connection.ConnectionState.DISCONNECTED}.</p>
	 * 
	 * @param eventListener A {@link ConnectionEventListener} that will receive connection events. This can be null if you are not interested in receiving connection events, in which case you should call {@link #connect()} instead of this method.
	 * @param connectionStates An optional list of {@link ConnectionState}s to bind your {@link ConnectionEventListener} to before connecting to Pusher. If you do not specify any {@link ConnectionState}s then your {@link ConnectionEventListener} will be bound to all connection events. This is equivalent to calling {@link #connect(ConnectionEventListener, ConnectionState...)} with {@link ConnectionState#ALL}.
	 * @throws IllegalArgumentException If the {@link ConnectionEventListener} is null and at least one connection state has been specified.
	 */
	public void connect(ConnectionEventListener eventListener, ConnectionState... connectionStates) {

		if (eventListener != null) {
			if (connectionStates.length == 0) {
				connectionStates = new ConnectionState[] { ConnectionState.ALL };
			}

			for (ConnectionState state : connectionStates) {
				connection.bind(state, eventListener);
			}
		} else {
			if (connectionStates.length > 0) {
				throw new IllegalArgumentException("Cannot bind to connection states with a null connection event listener");
			}
		}

		connection.connect();
	}

	/**
	 * Disconnect from Pusher.
	 * 
	 * <p>Calls are ignored if the {@link Pusher.getConnection().getState()} is not {@link com.pusher.client.connection.ConnectionState.CONNECTED}.</p>
	 */
	public void disconnect() {
		if (connection.getState() == ConnectionState.CONNECTED) {
			connection.disconnect();
		}
	}

	/* Subscription methods */

	/**
	 * Subscribes to a public {@link Channel}.
	 * 
	 * @param channelName The name of the {@link Channel} to subscribe to.
	 * @return The {@link Channel} object representing your subscription.
	 */
	public Channel subscribe(String channelName) {
		return subscribe(channelName, null);
	}

	/**
	 * Binds a {@link ChannelEventListener} to the specified events and then subscribes to a public {@link Channel}.
	 * 
	 * @param channelName The name of the {@link Channel} to subscribe to.
	 * @param listener A {@link ChannelEventListener} to receive events. This can be null if you don't want to bind a listener at subscription time, in which case you should call {@link #subscribe(String)} instead of this method.
	 * @param eventNames An optional list of event names to bind your {@link ChannelEventListener} to before subscribing.
	 * @return The {@link Channel} object representing your subscription.
	 * @throws IllegalArgumentException If any of the following are true:
	 *             <ul>
	 *             <li>The channel name is null.</li>
	 *             <li>You are already subscribed to this channel.</li>
	 *             <li>The channel name starts with "private-". If you want to subscribe to a private channel, call {@link #subscribePrivate(String, PrivateChannelEventListener, String...)} instead of this method.</li>
	 *             <li>At least one of the specified event names is null.</li>
	 *             <li>You have specified at least one event name and your {@link ChannelEventListener} is null.</li>
	 *             </ul>
	 */
	public Channel subscribe(String channelName, ChannelEventListener listener, String... eventNames) {

		InternalChannel channel = factory.newPublicChannel(channelName);
		channelManager.subscribeTo(channel, listener, eventNames);

		return channel;
	}

	/**
	 * Subscribes to a {@link com.pusher.client.channel.PrivateChannel} which requires authentication.
	 * 
	 * @param channelName The name of the channel to subscribe to.
	 * @return A new {@link com.pusher.client.channel.PrivateChannel} representing the subscription.
	 * @throws IllegalStateException if a {@link com.pusher.client.Authorizer} has not been set for the {@link Pusher} instance via {@link #Pusher(String, PusherOptions)}.
	 */
	public PrivateChannel subscribePrivate(String channelName) {
		return subscribePrivate(channelName, null);
	}

	/**
	 * Subscribes to a {@link com.pusher.client.channel.PrivateChannel} which requires authentication.
	 * 
	 * @param channelName The name of the channel to subscribe to.
	 * @param listener A listener to be informed of both Pusher channel protocol events and subscription data events.
	 * @param eventNames An optional list of names of events to be bound to on the channel. The equivalent of calling {@link com.pusher.client.channel.Channel.bind(String, SubscriptionListener)} on or more times.
	 * @return A new {@link com.pusher.client.channel.PrivateChannel} representing the subscription.
	 * @throws IllegalStateException if a {@link com.pusher.client.Authorizer} has not been set for the {@link Pusher} instance via {@link #Pusher(String, PusherOptions)}.
	 */
	public PrivateChannel subscribePrivate(String channelName, PrivateChannelEventListener listener, String... eventNames) {

		throwExceptionIfNoAuthorizerHasBeenSet();

		PrivateChannelImpl channel = factory.newPrivateChannel(connection, channelName, pusherOptions.getAuthorizer());
		channelManager.subscribeTo(channel, listener, eventNames);

		return channel;
	}

	/**
	 * Subscribes to a {@link com.pusher.client.channel.PresenceChannel} which requires authentication.
	 * 
	 * @param channelName The name of the channel to subscribe to.
	 * @return A new {@link com.pusher.client.channel.PresenceChannel} representing the subscription.
	 * @throws IllegalStateException if a {@link com.pusher.client.Authorizer} has not been set for the {@link Pusher} instance via {@link #Pusher(String, PusherOptions)}.
	 */
	public PresenceChannel subscribePresence(String channelName) {
		return subscribePresence(channelName, null);
	}

	/**
	 * Subscribes to a {@link com.pusher.client.channel.PresenceChannel} which requires authentication.
	 * 
	 * @param channelName The name of the channel to subscribe to.
	 * @param listener A listener to be informed of Pusher channel protocol, including presence-specific events, and subscription data events.
	 * @param eventNames An optional list of names of events to be bound to on the channel. The equivalent of calling {@link com.pusher.client.channel.Channel.bind(String, SubscriptionListener)} on or more times.
	 * @return A new {@link com.pusher.client.channel.PresenceChannel} representing the subscription.
	 * @throws IllegalStateException if a {@link com.pusher.client.Authorizer} has not been set for the {@link Pusher} instance via {@link #Pusher(String, PusherOptions)}.
	 */
	public PresenceChannel subscribePresence(String channelName, PresenceChannelEventListener listener, String... eventNames) {

		throwExceptionIfNoAuthorizerHasBeenSet();

		PresenceChannelImpl channel = factory.newPresenceChannel(connection, channelName, pusherOptions.getAuthorizer());
		channelManager.subscribeTo(channel, listener, eventNames);

		return channel;
	}

    public PresenceChannel subscribePermanent(String channelName) {
        return subscribePermanent(channelName, null);
    }

    public PresenceChannel subscribePermanent(String channelName, PresenceChannelEventListener listener, String... eventNames) {

        throwExceptionIfNoAuthorizerHasBeenSet();
        PresenceChannelImpl channel = factory.newPresenceChannel(connection, channelName, pusherOptions.getAuthorizer());
        channelManager.subscribeTo(channel, listener, eventNames);

        return channel;
    }

	/**
	 * Unsubscribes from a channel using via the name of the channel.
	 * @param channelName the name of the channel to be unsubscribed from.
	 * 
	 * @throws IllegalStateException if {@link Pusher.getConnection().getState()} is not {@link com.pusher.client.connection.ConnectionState.CONNECTED CONNECTED}
	 */
	public void unsubscribe(String channelName) {

		if (connection.getState() != ConnectionState.CONNECTED) {
			throw new IllegalStateException("Cannot unsubscribe from channel " + channelName + " while not connected");
		}

		channelManager.unsubscribeFrom(channelName);
	}

	/* implementation detail */

	private void throwExceptionIfNoAuthorizerHasBeenSet() {
		if (pusherOptions.getAuthorizer() == null) {
			throw new IllegalStateException("Cannot subscribe to a private or presence channel because no Authorizer has been set. Call PusherOptions.setAuthorizer() before connecting to Pusher");
		}
	}
}