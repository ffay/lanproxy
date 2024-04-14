package cn.dxbtech.portbridge.commons;

public class CommandLineUtil {
    private final String[] args;

    public CommandLineUtil(String[] args) {
        this.args = args;
    }

    public String get(String type) {

        if (args == null || args.length == 0) {
            return null;
        }

        for (int i = 0; i < args.length; i++) {
            if (StringUtil.equals(type, args[i])) {
                if (i + 1 >= args.length || args[i + 1].startsWith("-")) {
                    return null;
                }
                return StringUtil.trim(args[i + 1]);
            }
        }

        return null;
    }

    public boolean contains(String type) {
        return StringUtil.isNotEmpty(get(type));
    }

    public boolean containsKey(String key) {
        if (args == null || args.length == 0) {
            return false;
        }

        for (int i = 0; i < args.length; i++) {
            if (StringUtil.equals(key, args[i])) {
                return true;
            }
        }
        return false;
    }


    private StringBuilder help = new StringBuilder("\r\nHelp Information:");


    public void addHelp(String k1, String k2, String info) {
        help.append("\r\n").append("\t").append(k1).append("\t").append(k2).append(" , ").append(info);
    }

    public String help() {
        return help.toString();
    }
}
