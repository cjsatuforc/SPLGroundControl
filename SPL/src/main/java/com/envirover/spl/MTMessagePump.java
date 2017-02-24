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
along with Rock7MAVLink.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.envirover.spl;

import java.io.IOException;

import com.MAVLink.MAVLinkPacket;
import com.envirover.mavlink.MAVLinkChannel;

/*
 * Mobile-terminated message pump.
 */
public class MTMessagePump implements Runnable {

    private final MAVLinkChannel src;
    private final MAVLinkChannel dst;

    /*
     * @param src source messages channel
     * @param dst destination messages channel
     */
    public MTMessagePump(MAVLinkChannel src, MAVLinkChannel dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public void run() {
        System.out.println("MTMessagePump Started.");

        while(true) {
            try {
                MAVLinkPacket packet = src.receiveMessage();

                if (packet != null) {
                    System.out.printf("MT message received: msgid = %d.", packet.msgid);
                    System.out.println();

                    //TODO: filter out high frequency messages
                    dst.sendMessage(packet);
                }

                Thread.sleep(10);
            } catch(IOException ex) {
                ex.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("MTMessagePump Exited.");
                return;
            }
        }
    }

}
