/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ChannelInfoEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.HalfOpEvent;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.ModeEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.OpEvent;
import org.pircbotx.hooks.events.OwnerEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.RemoveChannelBanEvent;
import org.pircbotx.hooks.events.RemoveChannelKeyEvent;
import org.pircbotx.hooks.events.RemoveChannelLimitEvent;
import org.pircbotx.hooks.events.RemoveInviteOnlyEvent;
import org.pircbotx.hooks.events.RemoveModeratedEvent;
import org.pircbotx.hooks.events.RemoveNoExternalMessagesEvent;
import org.pircbotx.hooks.events.RemovePrivateEvent;
import org.pircbotx.hooks.events.RemoveSecretEvent;
import org.pircbotx.hooks.events.RemoveTopicProtectionEvent;
import org.pircbotx.hooks.events.SetChannelBanEvent;
import org.pircbotx.hooks.events.SetChannelKeyEvent;
import org.pircbotx.hooks.events.SetChannelLimitEvent;
import org.pircbotx.hooks.events.SetInviteOnlyEvent;
import org.pircbotx.hooks.events.SetModeratedEvent;
import org.pircbotx.hooks.events.SetNoExternalMessagesEvent;
import org.pircbotx.hooks.events.SetPrivateEvent;
import org.pircbotx.hooks.events.SetSecretEvent;
import org.pircbotx.hooks.events.SetTopicProtectionEvent;
import org.pircbotx.hooks.events.SuperOpEvent;
import org.pircbotx.hooks.events.TopicEvent;
import org.pircbotx.hooks.events.UserListEvent;
import org.pircbotx.hooks.events.UserModeEvent;
import org.pircbotx.hooks.events.VoiceEvent;
import org.smokinmils.bot.events.Action;
import org.smokinmils.bot.events.ChannelInfo;
import org.smokinmils.bot.events.Connect;
import org.smokinmils.bot.events.Disconnect;
import org.smokinmils.bot.events.HalfOp;
import org.smokinmils.bot.events.Invite;
import org.smokinmils.bot.events.Join;
import org.smokinmils.bot.events.Kick;
import org.smokinmils.bot.events.Message;
import org.smokinmils.bot.events.Mode;
import org.smokinmils.bot.events.NickChange;
import org.smokinmils.bot.events.Notice;
import org.smokinmils.bot.events.Op;
import org.smokinmils.bot.events.Owner;
import org.smokinmils.bot.events.Part;
import org.smokinmils.bot.events.PrivateMessage;
import org.smokinmils.bot.events.Quit;
import org.smokinmils.bot.events.RemoveChannelBan;
import org.smokinmils.bot.events.RemoveChannelKey;
import org.smokinmils.bot.events.RemoveChannelLimit;
import org.smokinmils.bot.events.RemoveInviteOnly;
import org.smokinmils.bot.events.RemoveModerated;
import org.smokinmils.bot.events.RemoveNoExternalMessages;
import org.smokinmils.bot.events.RemovePrivate;
import org.smokinmils.bot.events.RemoveSecret;
import org.smokinmils.bot.events.RemoveTopicProtection;
import org.smokinmils.bot.events.SetChannelBan;
import org.smokinmils.bot.events.SetChannelKey;
import org.smokinmils.bot.events.SetChannelLimit;
import org.smokinmils.bot.events.SetInviteOnly;
import org.smokinmils.bot.events.SetModerated;
import org.smokinmils.bot.events.SetNoExternalMessages;
import org.smokinmils.bot.events.SetPrivate;
import org.smokinmils.bot.events.SetSecret;
import org.smokinmils.bot.events.SetTopicProtection;
import org.smokinmils.bot.events.SuperOp;
import org.smokinmils.bot.events.Topic;
import org.smokinmils.bot.events.UserList;
import org.smokinmils.bot.events.UserMode;
import org.smokinmils.bot.events.Voice;

/**
 * The events class.
 * 
 * @author Jamie Reid
 */
public class Event extends ListenerAdapter<IrcBot> implements Listener<IrcBot> {
    /** The list of valid Channels this event is allowed to execute in. */
	private List<String> validChannels;
	
	/**
	 * Constructor.
	 * 
	 * @see org.pircbotx.hooks.ListenerAdapter
	 */
	public Event() {
		super();
		validChannels = new ArrayList<String>();
	}

	/**
	 * NOT TO BE USED.
	 * Redirect from the PircBotX event system.
	 * 
	 * @param event the ActionEvent
	 * 
	 * @throws Exception when anything goes wrong in the event implementation
	 */
	public final void onAction(final ActionEvent<IrcBot> event)
	        throws Exception {
		this.action(new Action(event));
	}
	
	/**
	 * Abstract event.
	 * 
	 * @param event the event.
	 * 
	 * @throws Exception thrown when something goes wrong in the implementation.
	 */
	public void action(final Action event) throws Exception { }
	
	/**
	 * NOT TO BE USED.
	 * Redirect from the PircBotX event system
     * 
     * @param event the ChannelInfoEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
	 */
	public final void onChannelInfo(final ChannelInfoEvent<IrcBot> event)
	        throws Exception {
		this.channelInfo(new ChannelInfo(event));
	}
    
    /**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */	
	public void channelInfo(final ChannelInfo event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the ConnectEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onConnect(final ConnectEvent<IrcBot> event)
	        throws Exception {
		this.connect(new Connect(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */ 
    public void connect(final Connect event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the DisconnectEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onDisconnect(final DisconnectEvent<IrcBot> event)
	        throws Exception {
		this.disconnect(new Disconnect(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */ 
    public void disconnect(final Disconnect event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the HalfOpEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onHalfOp(final HalfOpEvent<IrcBot> event)
	        throws Exception {
		this.halfOp(new HalfOp(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */    
	public void halfOp(final HalfOp event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the InviteEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onInvite(final InviteEvent<IrcBot> event)
	        throws Exception {
		this.invite(new Invite(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void invite(final Invite event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the JoinEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onJoin(final JoinEvent<IrcBot> event)
	        throws Exception {
		this.join(new Join(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void join(final Join event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the KickEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onKick(final KickEvent<IrcBot> event)
	        throws Exception {
		this.kick(new Kick(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void kick(final Kick event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the MessageEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onMessage(final MessageEvent<IrcBot> event)
	        throws Exception {
		this.message(new Message(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void message(final Message event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the ModeEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onMode(final ModeEvent<IrcBot> event) throws Exception {
		this.mode(new Mode(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void mode(final Mode event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the NickChangeEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onNickChange(final NickChangeEvent<IrcBot> event)
	        throws Exception {
		this.nickChange(new NickChange(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void nickChange(final NickChange event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the NoticeEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onNotice(final NoticeEvent<IrcBot> event)
	        throws Exception {
		this.notice(new Notice(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void notice(final Notice event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the OpEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onOp(final OpEvent<IrcBot> event) throws Exception {
		this.op(new Op(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void op(final Op event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the OwnerEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onOwner(final OwnerEvent<IrcBot> event) throws Exception {
		this.owner(new Owner(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void owner(final Owner event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the PartEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onPart(final PartEvent<IrcBot> event) throws Exception {
		this.part(new Part(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void part(final Part event) throws Exception { }	
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the PrivateMessageEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onPrivateMessage(final PrivateMessageEvent<IrcBot> event)
	        throws Exception {
		this.privateMessage(new PrivateMessage(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void privateMessage(final PrivateMessage event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the QuitEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onQuit(final QuitEvent<IrcBot> event) throws Exception {
		this.quit(new Quit(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void quit(final Quit event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the RemoveChannelBanEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onRemoveChannelBan(
	        final RemoveChannelBanEvent<IrcBot> event) throws Exception {
		this.removeChannelBan(new RemoveChannelBan(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void removeChannelBan(final RemoveChannelBan event)
	        throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the RemoveChannelKeyEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onRemoveChannelKey(
	        final RemoveChannelKeyEvent<IrcBot> event) throws Exception {
		this.removeChannelKey(new RemoveChannelKey(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void removeChannelKey(final RemoveChannelKey event)
	        throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the RemoveChannelLimitEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onRemoveChannelLimit(
	        final RemoveChannelLimitEvent<IrcBot> event) throws Exception {
		this.removeChannelLimit(new RemoveChannelLimit(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void removeChannelLimit(final RemoveChannelLimit event)
	        throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the RemoveInviteOnlyEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onRemoveInviteOnly(
	        final RemoveInviteOnlyEvent<IrcBot> event) throws Exception {
		this.removeInviteOnly(new RemoveInviteOnly(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void removeInviteOnly(final RemoveInviteOnly event)
	        throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the RemoveModeratedEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onRemoveModerated(
	        final RemoveModeratedEvent<IrcBot> event) throws Exception {
		this.removeModerated(new RemoveModerated(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void removeModerated(final RemoveModerated event)
            throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the RemoveNoExternalMessagesEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onRemoveNoExternalMessages(
	        final RemoveNoExternalMessagesEvent<IrcBot> event)
	                throws Exception {
		this.removeNoExternalMessages(new RemoveNoExternalMessages(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void removeNoExternalMessages(final RemoveNoExternalMessages event)
            throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the RemovePrivateEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onRemovePrivate(final RemovePrivateEvent<IrcBot> event)
	        throws Exception {
		this.removePrivate(new RemovePrivate(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void removePrivate(final RemovePrivate event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the RemoveSecretEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onRemoveSecret(final RemoveSecretEvent<IrcBot> event)
	        throws Exception {
		this.removeSecret(new RemoveSecret(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void removeSecret(final RemoveSecret event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the RemoveTopicProtectionEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onRemoveTopicProtection(
	        final RemoveTopicProtectionEvent<IrcBot> event) throws Exception {
		this.removeTopicProtection(new RemoveTopicProtection(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void removeTopicProtection(final RemoveTopicProtection event)
	        throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the SetChannelBanEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onSetChannelBan(final SetChannelBanEvent<IrcBot> event)
	        throws Exception {
		this.setChannelBan(new SetChannelBan(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void setChannelBan(final SetChannelBan event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the SetChannelKeyEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onSetChannelKey(final SetChannelKeyEvent<IrcBot> event)
	        throws Exception {
		this.setChannelKey(new SetChannelKey(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void setChannelKey(final SetChannelKey event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the SetChannelLimitEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onSetChannelLimit(
	        final SetChannelLimitEvent<IrcBot> event) throws Exception {
		this.setChannelLimit(new SetChannelLimit(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void setChannelLimit(final SetChannelLimit event)
            throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the SetInviteOnlyEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onSetInviteOnly(final SetInviteOnlyEvent<IrcBot> event)
	        throws Exception {
		this.setInviteOnly(new SetInviteOnly(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void setInviteOnly(final SetInviteOnly event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the SetModeratedEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onSetModerated(final SetModeratedEvent<IrcBot> event)
	        throws Exception {
		this.setModerated(new SetModerated(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void setModerated(final SetModerated event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the SetNoExternalMessagesEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onSetNoExternalMessages(
	        final SetNoExternalMessagesEvent<IrcBot> event) throws Exception {
		this.setNoExternalMessages(new SetNoExternalMessages(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void setNoExternalMessages(final SetNoExternalMessages event)
            throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the SetPrivateEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onSetPrivate(final SetPrivateEvent<IrcBot> event)
	        throws Exception {
		this.setPrivate(new SetPrivate(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void setPrivate(final SetPrivate event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the SetSecretEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onSetSecret(final SetSecretEvent<IrcBot> event)
	        throws Exception {
		this.setSecret(new SetSecret(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void setSecret(final SetSecret event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the SetTopicProtectionEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onSetTopicProtection(
	        final SetTopicProtectionEvent<IrcBot> event) throws Exception {
		this.setTopicProtection(new SetTopicProtection(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void setTopicProtection(final SetTopicProtection event)
	        throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the SuperOpEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onSuperOp(final SuperOpEvent<IrcBot> event)
	        throws Exception {
		this.superOp(new SuperOp(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void superOp(final SuperOp event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the TopicEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onTopic(final TopicEvent<IrcBot> event) throws Exception {
		this.topic(new Topic(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void topic(final Topic event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the UserListEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onUserList(final UserListEvent<IrcBot> event)
	        throws Exception {
		this.userList(new UserList(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void userList(final UserList event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the UserModeEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onUserMode(final UserModeEvent<IrcBot> event)
	        throws Exception {
		this.userMode(new UserMode(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void userMode(final UserMode event) throws Exception { }
	
    /**
     * NOT TO BE USED.
     * Redirect from the PircBotX event system.
     * 
     * @param event the VoiceEvent
     * 
     * @throws Exception when anything goes wrong in the event implementation
     */
	public final void onVoice(final VoiceEvent<IrcBot> event) throws Exception {
		this.voice(new Voice(event));
	}
	
	/**
     * Abstract event.
     * 
     * @param event the event.
     * 
     * @throws Exception thrown when something goes wrong in the implementation.
     */
    public void voice(final Voice event) throws Exception { }
	
	/**
	 * Adds a channel that this listener/command is used in.
	 * @param channel the channel name
	 */
	public final void addValidChan(final String channel) {
		validChannels.add(channel.toLowerCase());
	}
	
	/**
	 * Adds the channels that this listener/command is used in. 
	 * @param channels the array of all channels to add
	 * @return 
	 */
	public final void addValidChan(final String[] channels) {
		for (String chan: channels)  {
			addValidChan(chan.toLowerCase());
		}
	}
	
	/**
	 * Checks if the provided channel allows this listener to be used.
	 * 
	 * @param channel the channel to check
	 * 
	 * @return true if this listener is active in the channel, false otherwise
	 */
	public final boolean isValidChannel(final String channel) {
		return validChannels.contains(channel.toLowerCase());
	}
}
