/*
This file is part of SPLGroundControl application.

SPLGroundControl is a MAVLink proxy server for ArduPilot rovers with
RockBLOCK satellite communication.

See http://www.rock7mobile.com/downloads/RockBLOCK-Web-Services-User-Guide.pdf

Copyright (C) 2017 Envirover

SPLGroundControl is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SPLGroundControl is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Rock7MAVLink.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.envirover.mavlink;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.common.msg_command_ack;
import com.MAVLink.common.msg_command_int;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.common.msg_mission_ack;
import com.MAVLink.common.msg_mission_clear_all;
import com.MAVLink.common.msg_mission_count;
import com.MAVLink.common.msg_mission_current;
import com.MAVLink.common.msg_mission_item;
import com.MAVLink.common.msg_mission_item_int;
import com.MAVLink.common.msg_mission_request;
import com.MAVLink.common.msg_mission_request_int;
import com.MAVLink.common.msg_mission_request_list;
import com.MAVLink.common.msg_mission_request_partial_list;
import com.MAVLink.common.msg_mission_set_current;
import com.MAVLink.common.msg_mission_write_partial_list;
import com.MAVLink.common.msg_param_set;
import com.MAVLink.common.msg_set_home_position;
import com.MAVLink.common.msg_set_mode;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_MISSION_RESULT;
import com.MAVLink.enums.MAV_RESULT;

/**
 * Receives MAVLink message packets from a source channel, such as MAVLinkSocket,
 * filters out high frequency messages, and sends the packets to the destination
 * channel, such as MAVLinkMessageQueue.
 */
public class MAVLinkHandler implements Runnable {

    private final static Logger logger = Logger.getLogger(MAVLinkHandler.class);

    private final MAVLinkChannel src;
    private final MAVLinkChannel dst;
    private int missionCount = 0;

    /**
     * Constructs instance of MAVLinkHandler.
     * 
     * @param src source message channel, such as MAVLinkSocket.
     * @param dst destination message channel, such as MAVLinkMessageQueue.
     */
    public MAVLinkHandler(MAVLinkChannel src, MAVLinkChannel dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public void run() {
        logger.debug("MAVLinkHandler started.");

        while (true) {
            try {
                MAVLinkPacket packet = src.receiveMessage();

                if (filter(packet)) {
                    handleMissionWrite(packet);
                    handleCommand(packet);

                    dst.sendMessage(packet);
                }

                Thread.sleep(10);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } catch (InterruptedException e) {
                logger.debug("MAVLinkHandler interrupted.");
                return;
            }
        }
    }
    
    private void handleMissionWrite(MAVLinkPacket packet) throws IOException {
        if (packet.msgid == msg_mission_count.MAVLINK_MSG_ID_MISSION_COUNT) {
            msg_mission_count msg = (msg_mission_count)(packet.unpack());
            missionCount = msg.count;
            msg_mission_request mission_request = new msg_mission_request();
            mission_request.seq = 0;
            mission_request.sysid = msg.target_system;
            mission_request.compid = msg.target_component;
            mission_request.target_system = (short)packet.sysid;
            mission_request.target_component = (short)packet.compid;
            src.sendMessage(mission_request.pack());
        } else if (packet.msgid == msg_mission_item.MAVLINK_MSG_ID_MISSION_ITEM) {
            msg_mission_item msg = (msg_mission_item)(packet.unpack());
            if (msg.seq + 1 < missionCount) {
                msg_mission_request mission_request = new msg_mission_request();
                mission_request.seq = msg.seq + 1;
                mission_request.sysid = msg.target_system;
                mission_request.compid = msg.target_component;
                mission_request.target_system = (short)packet.sysid;
                mission_request.target_component = (short)packet.compid;
                src.sendMessage(mission_request.pack());
            } else {
                msg_mission_ack mission_ack = new msg_mission_ack();
                mission_ack.type = MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED;
                mission_ack.sysid = msg.target_system;
                mission_ack.compid = msg.target_component;
                mission_ack.target_system = (short)packet.sysid;
                mission_ack.target_component = (short)packet.compid;
                src.sendMessage(mission_ack.pack());
                missionCount = 0;
            }
        }
    }

    /**
     * Immediately return COMMAND_ACK for COMMAND_LONG and and COMMAND_INT.
     * 
     * @param packet
     * @throws IOException
     */
    private void handleCommand(MAVLinkPacket packet) throws IOException {
        if (packet.msgid == msg_command_long.MAVLINK_MSG_ID_COMMAND_LONG) {
            msg_command_long msg = (msg_command_long)packet.unpack();
            msg_command_ack command_ack = new msg_command_ack();
            command_ack.command = msg.command;
            command_ack.result = MAV_RESULT.MAV_RESULT_ACCEPTED;
            command_ack.sysid = msg.target_system;
            command_ack.compid = msg.target_component;
            src.sendMessage(command_ack.pack());
        } else if (packet.msgid == msg_command_int.MAVLINK_MSG_ID_COMMAND_INT) {
            msg_command_int msg = (msg_command_int)packet.unpack();
            msg_command_ack command_ack = new msg_command_ack();
            command_ack.command = msg.command;
            command_ack.result = MAV_RESULT.MAV_RESULT_ACCEPTED;
            command_ack.sysid = msg.target_system;
            command_ack.compid = msg.target_component;
            src.sendMessage(command_ack.pack());
        }
    }

    //White list message filter
    private static boolean filter(MAVLinkPacket packet) {
        return packet != null &&
              (packet.msgid == msg_set_mode.MAVLINK_MSG_ID_SET_MODE || 
               packet.msgid == msg_param_set.MAVLINK_MSG_ID_PARAM_SET ||
               packet.msgid == msg_mission_request_partial_list.MAVLINK_MSG_ID_MISSION_REQUEST_PARTIAL_LIST ||
               packet.msgid == msg_mission_write_partial_list.MAVLINK_MSG_ID_MISSION_WRITE_PARTIAL_LIST ||
               packet.msgid == msg_mission_item.MAVLINK_MSG_ID_MISSION_ITEM ||
               packet.msgid == msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST ||
               packet.msgid == msg_mission_set_current.MAVLINK_MSG_ID_MISSION_SET_CURRENT ||
               packet.msgid == msg_mission_current.MAVLINK_MSG_ID_MISSION_CURRENT ||
               packet.msgid == msg_mission_request_list.MAVLINK_MSG_ID_MISSION_REQUEST_LIST ||
               packet.msgid == msg_mission_count.MAVLINK_MSG_ID_MISSION_COUNT ||
               packet.msgid == msg_mission_clear_all.MAVLINK_MSG_ID_MISSION_CLEAR_ALL ||
               packet.msgid == msg_mission_request_int.MAVLINK_MSG_ID_MISSION_REQUEST_INT ||
               packet.msgid == msg_mission_item_int.MAVLINK_MSG_ID_MISSION_ITEM_INT ||
               packet.msgid == msg_command_int.MAVLINK_MSG_ID_COMMAND_INT ||
              (packet.msgid == msg_command_long.MAVLINK_MSG_ID_COMMAND_LONG && 
               ((msg_command_long)packet.unpack()).command != MAV_CMD.MAV_CMD_REQUEST_AUTOPILOT_CAPABILITIES) ||
               packet.msgid == msg_set_home_position.MAVLINK_MSG_ID_SET_HOME_POSITION);
    }
    
    
    /* (blacklist filter)
    // Filter out high frequency messages 
    private static boolean filter(MAVLinkPacket packet) {
        return packet != null &&
               packet.msgid != msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT &&
               packet.msgid != msg_param_request_list.MAVLINK_MSG_ID_PARAM_REQUEST_LIST &&
               packet.msgid != msg_request_data_stream.MAVLINK_MSG_ID_REQUEST_DATA_STREAM;
    }
    */
    
}
