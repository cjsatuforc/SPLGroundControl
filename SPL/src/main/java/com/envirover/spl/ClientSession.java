/*
This file is part of SPLGroundControl application.

SPLGroundControl is a ground control proxy station for ArduPilot rovers with
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
along with SPLGroundControl.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.envirover.spl;

import java.io.IOException;

import com.MAVLink.MAVLinkPacket;

/*
 * TCP and WebSocket MAVLink client sessions that handle communications with GCS clients.
 *
 */
public interface ClientSession {

    /**
     * Called when client is connected.
     */
    void onOpen();

    /**
     * Called when client is disconnected.
     * 
     * @throws InterruptedException
     */
    void onClose() throws InterruptedException;

    /**
     * Called when MAVLink message is received from the client.
     * 
     * @param packet MAVLink packet 
     * @throws IOException thrown in case of I/O errors.
     * @throws InterruptedException
     */
    void onMessage(MAVLinkPacket packet) throws IOException, InterruptedException;

}