/*
This file is part of SPL GroundControl application.

SPL GroundControl is a ground control proxy station for ArduPilot rovers with
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

import com.envirover.mavlink.MAVLinkSocket;

/**
 * TCP server that accepts connections from GCS clients to update on-board parameters
 * and missions in the reported state of the shadow.
 * 
 * {@link com.envirover.spl.ShadowClientSession} is created for each client connection. 
 *  
 * @author pavel
 *
 */
public class ShadowTcpServer extends MAVLinkTcpServer {
    
    /**
     * Creates an instance of ShadowTcpServer 
     * 
     * @param port TCP port used for MAVLink ground control stations connections 
     */
    public ShadowTcpServer(Integer port) {
        super(port, null);
    }

    @Override
    protected ClientSession createClientSession(MAVLinkSocket clientSocket) {
        return new ShadowClientSession(clientSocket);
    }

}
