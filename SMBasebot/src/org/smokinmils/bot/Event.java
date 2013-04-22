/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter ;
import org.smokinmils.bot.events.*;

/**
 * The events class
 * 
 * @author Jamie Reid
 */
public class Event extends ListenerAdapter<PircBotX> implements Listener<PircBotX> {
	public void onAction(Action event) throws Exception {}
	public void onChannelInfo(ChannelInfo event) throws Exception {}
	public void onConnect(Connect event) throws Exception {}
	public void onDisconnect(Disconnect event) throws Exception {}
	public void onHalfOp(HalfOp event) throws Exception {}
	public void onInvite(Invite event) throws Exception {}
	public void onJoin(Join event) throws Exception {}
	public void onKick(Kick event) throws Exception {}
	public void onMessage(Message event) throws Exception {}	
	public void onMode(Mode event) throws Exception {}
	public void onNickChange(NickChange event) throws Exception {}
	public void onNotice(Notice event) throws Exception {}
	public void onOp(Op event) throws Exception {}
	public void onOwner(Owner event) throws Exception {}
	public void onPart(Part event) throws Exception {}
	public void onPrivateMessage(PrivateMessage event) throws Exception {}
	public void onQuit(Quit event) throws Exception {}
	public void onRemoveChannelBan(RemoveChannelBan event) throws Exception {}
	public void onRemoveChannelKey(RemoveChannelKey event) throws Exception {}
	public void onRemoveChannelLimit(RemoveChannelLimit event) throws Exception {}
	public void onRemoveInviteOnly(RemoveInviteOnly event) throws Exception {}
	public void onRemoveModerated(RemoveModerated event) throws Exception {}
	public void onRemoveNoExternalMessages(RemoveNoExternalMessages event) throws Exception {}
	public void onRemovePrivate(RemovePrivate event) throws Exception {}
	public void onRemoveSecret(RemoveSecret event) throws Exception {}
	public void onRemoveTopicProtection(RemoveTopicProtection event) throws Exception {}
	public void onSetChannelBan(SetChannelBan event) throws Exception {}
	public void onSetChannelKey(SetChannelKey event) throws Exception {}
	public void onSetChannelLimit(SetChannelLimit event) throws Exception {}
	public void onSetInviteOnly(SetInviteOnly event) throws Exception {}
	public void onSetModerated(SetModerated event) throws Exception {}
	public void onSetNoExternalMessages(SetNoExternalMessages event) throws Exception {}
	public void onSetPrivate(SetPrivate event) throws Exception {}
	public void onSetSecret(SetSecret event) throws Exception {}
	public void onSetTopicProtection(SetTopicProtection event) throws Exception {}
	public void onSuperOp(SuperOp event) throws Exception {}
	public void onTopic(Topic event) throws Exception {}
	public void onUserList(UserList event) throws Exception {}
	public void onUserMode(UserMode event) throws Exception {}
	public void onVoice(Voice event) throws Exception {}
}
