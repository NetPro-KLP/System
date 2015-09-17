package serverStarter;

import xmlReader.Handle;
import xmlReader.HandlerData;
import xmlReader.HandlerListData;
import xmlReader.ServerListData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import webServer.WebServer;
import firewallServer.FirewallServer;
import firewallServer.NioEventHandler;

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

            for (Handle handler : handlers) {
                firewallServer.registerHandler(handler.getHeader(), handler.getClassName());
            }

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

        try {
            Serializer serializer = new Persister();
            File xml = new File("HandlerList.xml");

            ServerListData serverList = serializer.read(ServerListData.class, xml);

            for (HandlerListData handlerListData : serverList.getServer()) {
                if (serverName.equals(handlerListData.getName())) {
                    List<HandlerData> handlerList = handlerListData.getHandler();
                    for (HandlerData handler : handlerList) {
                        handlers.add(new Handle(handler.getHeader(), handler.getHandler()));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return handlers;
    }
}
