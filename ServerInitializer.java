import xmlReader.Handle;
import xmlReader.HandlerData;
import xmlReader.HandlerListData;
import xmlReader.ServerListData;

import java.util.ArrayList;

import webServer.WebServer;
import firewallServer.FirewallServer;

public class ServerInitializer {

    public static void main (String[] args) {
        if (args.length != 1)
            System.exit(1);

        int firewallPort = 30000;
        int webPort = 20000;
        String serverName = args[0];

        if ("firewall".equals(serverName)) {
            FirewallServer firewallServer = new FirewallServer(firewallPort);

            ArrayList<Handle> handlers = getHandlerList(serverName);

            firewallServer.startServer();
        }
        else if ("web".equals(serverName)) {
        }
        else {
            System.out.println("Usage\n\t java ServerInitializer firewall");
            System.out.println("\t java ServerInitializer web\n");
            System.exit(1);
        }
    }

    private static ArrayList<Handle> getHandlerList(String serverName) {
        ArrayList<Handle> handlers = new ArrayList<Handle>();

        return handlers;
    }
}
