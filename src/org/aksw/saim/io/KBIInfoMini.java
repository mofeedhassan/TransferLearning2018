package org.aksw.saim.io;

import de.uni_leipzig.simba.io.KBInfo;

public class KBIInfoMini extends KBInfo {
	
    public int hashCode(int size) {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((endpoint == null) ? 0 : endpoint.hashCode());
        result = prime * result + ((graph == null) ? 0 : graph.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + pageSize;
        result = prime * result
                + ((prefixes == null) ? 0 : prefixes.hashCode());
        result = prime * result
                + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result
                + ((restrictions == null) ? 0 : restrictions.hashCode());
        //result = prime * result + ((var == null) ? 0 : var.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((size < 0) ? 0 : Integer.valueOf(size).hashCode());
        return result;
    }
}
