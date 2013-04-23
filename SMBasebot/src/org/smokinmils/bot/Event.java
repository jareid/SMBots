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
import org.pircbotx.hooks.ListenerAdapter ;
import org.pircbotx.hooks.events.*;
import org.smokinmils.bot.events.*;

/**
 * The events class
 * 
 * @author Jamie Reid
 */
public class Event extends ListenerAdapter<IrcBot> implements Listener<IrcBot> {
	List<String> validChannels;
	
	public Event() {
		super();
		validChannels = new ArrayList<String>();
	}

	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onAction(ActionEvent<IrcBot> event) throws Exception {
		this.action( new Action(event) );
	}
	
	public void action(Action event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onChannelInfo(ChannelInfoEvent<IrcBot> event) throws Exception {
		this.channelInfo( new ChannelInfo(event) );
	}
	
	public void channelInfo(ChannelInfo event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onConnect(ConnectEvent<IrcBot> event) throws Exception {
		this.connect( new Connect(event) );
	}
	
	public void connect(Connect event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onDisconnect(DisconnectEvent<IrcBot> event) throws Exception {
		this.disconnect( new Disconnect(event) );
	}
	
	public void disconnect(Disconnect event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onHalfOp(HalfOpEvent<IrcBot> event) throws Exception {
		this.halfOp( new HalfOp (event) );
	}
	
	public void halfOp(HalfOp event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onInvite(InviteEvent<IrcBot> event) throws Exception {
		this.invite( new Invite(event) );
	}
	
	public void invite(Invite event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onJoin(JoinEvent<IrcBot> event) throws Exception {
		this.join( new Join(event) );
	}
	
	public void join(Join event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onKick(KickEvent<IrcBot> event) throws Exception {
		this.kick( new Kick(event) );
	}
	
	public void kick(Kick event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onMessage(MessageEvent<IrcBot> event) throws Exception {
		this.message( new Message (event) );
	}
	
	public void message(Message event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onMode(ModeEvent<IrcBot> event) throws Exception {
		this.mode( new Mode(event) );
	}
	
	public void mode(Mode event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onNickChange(NickChangeEvent<IrcBot> event) throws Exception {
		this.nickChange( new NickChange(event) );
	}
	
	public void nickChange(NickChange event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onNotice(NoticeEvent<IrcBot> event) throws Exception {
		this.notice( new Notice(event) );
	}
	
	public void notice(Notice event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onOp(OpEvent<IrcBot> event) throws Exception {
		this.op( new Op(event) );
	}
	
	public void op(Op event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onOwner(OwnerEvent<IrcBot> event) throws Exception {
		this.owner( new Owner(event) );
	}
	
	public void owner(Owner event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onPart(PartEvent<IrcBot> event) throws Exception {
		this.part( new Part(event) );
	}
	
	public void part(Part event) throws Exception {}	
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onPrivateMessage(PrivateMessageEvent<IrcBot> event) throws Exception {
		this.privateMessage( new PrivateMessage(event) );
	}
	
	public void privateMessage(PrivateMessage event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onQuit(QuitEvent<IrcBot> event) throws Exception {
		this.quit( new Quit(event) );
	}
	
	public void quit(Quit event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onRemoveChannelBan(RemoveChannelBanEvent<IrcBot> event) throws Exception {
		this.removeChannelBan( new RemoveChannelBan(event) );
	}
	
	public void removeChannelBan(RemoveChannelBan event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onRemoveChannelKey(RemoveChannelKeyEvent<IrcBot> event) throws Exception {
		this.removeChannelKey( new RemoveChannelKey (event) );
	}
	
	public void removeChannelKey(RemoveChannelKey event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onRemoveChannelLimit(RemoveChannelLimitEvent<IrcBot> event) throws Exception {
		this.removeChannelLimit( new RemoveChannelLimit(event) );
	}
	
	public void removeChannelLimit(RemoveChannelLimit event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onRemoveInviteOnly(RemoveInviteOnlyEvent<IrcBot> event) throws Exception {
		this.removeInviteOnly( new RemoveInviteOnly(event) );
	}
	
	public void removeInviteOnly(RemoveInviteOnly event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onRemoveModerated(RemoveModeratedEvent<IrcBot> event) throws Exception {
		this.removeModerated( new RemoveModerated(event) );
	}
	
	public void removeModerated(RemoveModerated event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onRemoveNoExternalMessages(RemoveNoExternalMessagesEvent<IrcBot> event) throws Exception {
		this.removeNoExternalMessages( new RemoveNoExternalMessages(event) );
	}
	
	public void removeNoExternalMessages(RemoveNoExternalMessages event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onRemovePrivate(RemovePrivateEvent<IrcBot> event) throws Exception {
		this.removePrivate( new RemovePrivate(event) );
	}
	
	public void removePrivate(RemovePrivate event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onRemoveSecret(RemoveSecretEvent<IrcBot> event) throws Exception {
		this.removeSecret( new RemoveSecret(event) );
	}
	
	public void removeSecret(RemoveSecret event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onRemoveTopicProtection(RemoveTopicProtectionEvent<IrcBot> event) throws Exception {
		this.removeTopicProtection( new RemoveTopicProtection(event) );
	}
	
	public void removeTopicProtection(RemoveTopicProtection event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onSetChannelBan(SetChannelBanEvent<IrcBot> event) throws Exception {
		this.setChannelBan( new SetChannelBan(event) );
	}
	
	public void setChannelBan(SetChannelBan event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onSetChannelKey(SetChannelKeyEvent<IrcBot> event) throws Exception {
		this.setChannelKey( new SetChannelKey(event) );
	}
	
	public void setChannelKey(SetChannelKey event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onSetChannelLimit(SetChannelLimitEvent<IrcBot> event) throws Exception {
		this.setChannelLimit( new SetChannelLimit(event) );
	}
	
	public void setChannelLimit(SetChannelLimit event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onSetInviteOnly(SetInviteOnlyEvent<IrcBot> event) throws Exception {
		this.setInviteOnly( new SetInviteOnly(event) );
	}
	
	public void setInviteOnly(SetInviteOnly event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onSetModerated(SetModeratedEvent<IrcBot> event) throws Exception {
		this.setModerated( new SetModerated(event) );
	}
	
	public void setModerated(SetModerated event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onSetNoExternalMessages(SetNoExternalMessagesEvent<IrcBot> event) throws Exception {
		this.setNoExternalMessages( new SetNoExternalMessages(event) );
	}
	
	public void setNoExternalMessages(SetNoExternalMessages event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onSetPrivate(SetPrivateEvent<IrcBot> event) throws Exception {
		this.setPrivate( new SetPrivate(event) );
	}
	
	public void setPrivate(SetPrivate event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onSetSecret(SetSecretEvent<IrcBot> event) throws Exception {
		this.setSecret( new SetSecret(event) );
	}
	
	public void setSecret(SetSecret event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onSetTopicProtection(SetTopicProtectionEvent<IrcBot> event) throws Exception {
		this.setTopicProtection( new SetTopicProtection(event) );
	}
	
	public void setTopicProtection(SetTopicProtection event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onSuperOp(SuperOpEvent<IrcBot> event) throws Exception {
		this.superOp( new SuperOp(event) );
	}
	
	public void superOp(SuperOp event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onTopic(TopicEvent<IrcBot> event) throws Exception {
		this.topic( new Topic(event) );
	}
	
	public void topic(Topic event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onUserList(UserListEvent<IrcBot> event) throws Exception {
		this.userList( new UserList(event) );
	}
	
	public void userList(UserList event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onUserMode(UserModeEvent<IrcBot> event) throws Exception {
		this.userMode( new UserMode (event) );
	}
	
	public void userMode(UserMode event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	public void onVoice(VoiceEvent<IrcBot> event) throws Exception {
		this.voice( new Voice(event) );
	}
	
	public void voice(Voice event) throws Exception {}
	
	/**
	 * NOT TO BE USED
	 * Redirect from the PircBotX event system
	 * NOT TO BE USED
	 */
	
	/**
	 * Adds a channel that this listener/command is used in.
	 * @param channel the channel name
	 */
	public void addValidChan(String channel) {
		validChannels.add(channel);
	}
	
	/**
	 * Adds the channels that this listener/command is used in. 
	 * @param channels the array of all channels to add
	 * @return 
	 */
	public void addValidChan(String[] channels) {
		for (String chan: channels)  {
			validChannels.add(chan);
		}
	}
	
	/**
	 * Checks if the provided channel allows this listener to be used.
	 * 
	 * @param channel the channel to check
	 * 
	 * @return true if this listener is active in the channel, false otherwise
	 */
	public boolean isValidChannel(String channel) {
		return validChannels.contains(channel);
	}
}
