package com.comphenix.protocol;

import com.comphenix.protocol.PacketType.Protocol;
import com.comphenix.protocol.PacketType.Sender;
import com.comphenix.protocol.collections.IntegerMap;
import com.google.common.base.Preconditions;

/**
 * Retrieve a packet type based on its version and ID, optionally with protocol and sender too.
 * @author Kristian
 */
class PacketTypeLookup {
	private static class ProtocolSenderLookup {
		// Unroll lookup for performance reasons
		public final IntegerMap<PacketType> HANDSHAKE_CLIENT = IntegerMap.newMap();
		public final IntegerMap<PacketType> HANDSHAKE_SERVER = IntegerMap.newMap();
		public final IntegerMap<PacketType> GAME_CLIENT = IntegerMap.newMap();
		public final IntegerMap<PacketType> GAME_SERVER = IntegerMap.newMap();
		public final IntegerMap<PacketType> STATUS_CLIENT = IntegerMap.newMap();
		public final IntegerMap<PacketType> STATUS_SERVER = IntegerMap.newMap();
		public final IntegerMap<PacketType> LOGIN_CLIENT = IntegerMap.newMap();
		public final IntegerMap<PacketType> LOGIN_SERVER = IntegerMap.newMap();
		
		/**
		 * Retrieve the correct integer map for a specific protocol and sender.
		 * @param protocol - the protocol.
		 * @param sender - the sender.
		 * @return The integer map of packets.
		 */
		public IntegerMap<PacketType> getMap(Protocol protocol, Sender sender) {
			switch (protocol) {
				case HANDSHAKING: 
					return sender == Sender.CLIENT ? HANDSHAKE_CLIENT : HANDSHAKE_SERVER;
				case PLAY:
					return sender == Sender.CLIENT ? GAME_CLIENT : GAME_SERVER;
				case STATUS:
					return sender == Sender.CLIENT ? STATUS_CLIENT : STATUS_SERVER;
				case LOGIN:
					return sender == Sender.CLIENT ? LOGIN_CLIENT : LOGIN_SERVER;
				default:
					throw new IllegalArgumentException("Unable to find protocol " + protocol);
			}
		}
	}
	
	// Packet IDs from 1.6.4 and below
	private final IntegerMap<PacketType> legacyLookup = new IntegerMap<PacketType>();
	private final IntegerMap<PacketType> serverLookup = new IntegerMap<PacketType>();
	private final IntegerMap<PacketType> clientLookup = new IntegerMap<PacketType>();
	
	// Packets for 1.7.2
	private final ProtocolSenderLookup currentLookup = new ProtocolSenderLookup();

	/**
	 * Add a collection of packet types to the lookup.
	 * @param types - the types to add.
	 */
	public PacketTypeLookup addPacketTypes(Iterable<? extends PacketType> types) {
		Preconditions.checkNotNull(types, "types cannot be NULL");
		
		for (PacketType type : types) {
			int legacy = type.getLegacyId();
			
			// Skip unknown legacy packets
			if (legacy != PacketType.UNKNOWN_PACKET) {
				if (type.isServer())
					serverLookup.put(type.getLegacyId(), type);
				if (type.isClient())
					clientLookup.put(type.getLegacyId(), type);
				legacyLookup.put(type.getLegacyId(), type);
			}
			// Skip unknown current packets
			if (type.getCurrentId() != PacketType.UNKNOWN_PACKET) {
				currentLookup.getMap(type.getProtocol(), type.getSender()).put(type.getCurrentId(), type);
			}
		}
		return this;
	}
	
	/**
	 * Retrieve a packet type from a legacy (1.6.4 and below) packet ID.
	 * @param packetId - the legacy packet ID.
	 * @return The corresponding packet type, or NULL if not found.
	 */
	public PacketType getFromLegacy(int packetId) {
		return legacyLookup.get(packetId);
	}
	
	/**
	 * Retrieve a packet type from a legacy (1.6.4 and below) packet ID.
	 * @param packetId - the legacy packet ID.
	 * @param preference - which packet type to look for first.
	 * @return The corresponding packet type, or NULL if not found.
	 */
	public PacketType getFromLegacy(int packetId, Sender preference) {	
		if (preference == Sender.CLIENT)
			return getFirst(packetId, clientLookup, serverLookup);
		else
			return getFirst(packetId, serverLookup, clientLookup);
	}
	
	// Helper method for looking up in two sets
	private <T> T getFirst(int packetId, IntegerMap<T> first, IntegerMap<T> second) {
		if (first.containsKey(packetId))
			return first.get(packetId);
		else
			return second.get(packetId);
	}
	
	/**
	 * Retrieve a packet type from a protocol, sender and packet ID.
	 * @param protocol - the current protocol.
	 * @param sender - the sender.
	 * @param packetId - the packet ID.
	 * @return The corresponding packet type, or NULL if not found.
	 */
	public PacketType getFromCurrent(Protocol protocol, Sender sender, int packetId) {
		return currentLookup.getMap(protocol, sender).get(packetId);
	}
}
