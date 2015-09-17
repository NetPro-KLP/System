public class ServerInitializer {
    public static void main (String[] args) {
        if (args.length != 1)
            System.exit(1);

        int firewallPort = 30000;
        int adminPort = 20000;
        String serverName = args[0];

        if ("firewall".equals(serverName)) {
        }
        else if ("admin", equals(serverName)) {
        }
        else {
            System.exit(1);
        }
    }
}
