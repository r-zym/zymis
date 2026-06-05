package pl.sunglasses.zymis.util;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.sunglasses.zymis.ZymItemshopPlugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public class CommandUtil implements CommandExecutor, TabCompleter {

    private final ZymItemshopPlugin plugin;
    private final Map<String, CommandData> commands = new HashMap<>();
    private final Map<Class<?>, ArgumentResolver<?>> argumentResolvers = new HashMap<>();
    private CommandMap commandMap;

    public CommandUtil(ZymItemshopPlugin plugin) {
        this.plugin = plugin;
        this.commandMap = getCommandMap();
        registerDefaultResolvers();
    }

    private void registerDefaultResolvers() {
        registerArgumentResolver(String.class, new ArgumentResolver<String>() {
            @Override
            public String resolve(String input) { return input; }
            @Override
            public List<String> getDefaultSuggestions() { return Collections.singletonList("<tekst>"); }
        });

        registerArgumentResolver(Integer.class, new ArgumentResolver<Integer>() {
            @Override
            public Integer resolve(String input) {
                try { return Integer.parseInt(input); } catch (NumberFormatException e) { return null; }
            }
            @Override
            public List<String> getDefaultSuggestions() { return Arrays.asList("1", "5", "10", "100"); }
        });

        registerArgumentResolver(int.class, new ArgumentResolver<Integer>() {
            @Override
            public Integer resolve(String input) {
                try { return Integer.parseInt(input); } catch (NumberFormatException e) { return null; }
            }
            @Override
            public List<String> getDefaultSuggestions() { return Arrays.asList("1", "5", "10", "100"); }
        });

        registerArgumentResolver(Double.class, new ArgumentResolver<Double>() {
            @Override
            public Double resolve(String input) {
                try { return Double.parseDouble(input); } catch (NumberFormatException e) { return null; }
            }
            @Override
            public List<String> getDefaultSuggestions() { return Arrays.asList("1.0", "10.0"); }
        });

        registerArgumentResolver(double.class, new ArgumentResolver<Double>() {
            @Override
            public Double resolve(String input) {
                try { return Double.parseDouble(input); } catch (NumberFormatException e) { return null; }
            }
            @Override
            public List<String> getDefaultSuggestions() { return Arrays.asList("1.0", "10.0"); }
        });

        registerArgumentResolver(Boolean.class, new ArgumentResolver<Boolean>() {
            @Override
            public Boolean resolve(String input) {
                if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("tak")) return true;
                if (input.equalsIgnoreCase("false") || input.equalsIgnoreCase("nie")) return false;
                return null;
            }
            @Override
            public List<String> getDefaultSuggestions() { return Arrays.asList("true", "false"); }
        });

        registerArgumentResolver(boolean.class, new ArgumentResolver<Boolean>() {
            @Override
            public Boolean resolve(String input) {
                if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("tak")) return true;
                if (input.equalsIgnoreCase("false") || input.equalsIgnoreCase("nie")) return false;
                return null;
            }
            @Override
            public List<String> getDefaultSuggestions() { return Arrays.asList("true", "false"); }
        });

        registerArgumentResolver(Player.class, new ArgumentResolver<Player>() {
            @Override
            public Player resolve(String input) { return Bukkit.getPlayer(input); }
            @Override
            public List<String> getDefaultSuggestions() {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            }
        });
    }

    public <T> void registerArgumentResolver(Class<T> type, ArgumentResolver<T> resolver) {
        argumentResolvers.put(type, resolver);
    }

    private CommandMap getCommandMap() {
        try {
            return Bukkit.getCommandMap();
        } catch (Exception e) {
            try {
                Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                field.setAccessible(true);
                return (CommandMap) field.get(Bukkit.getServer());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public void registerCommand(Object commandClass) {
        Class<?> clazz = commandClass.getClass();
        CommandInfo main = clazz.getAnnotation(CommandInfo.class);
        if (main == null) return;

        String name = main.name().toLowerCase();
        CommandData data = new CommandData(name, main.description(), main.usage(), main.aliases(), main.permission());

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ExecuteCommand.class)) {
                ExecuteCommand exec = method.getAnnotation(ExecuteCommand.class);
                RequirePermission perm = method.getAnnotation(RequirePermission.class);
                PlayerOnly po = method.getAnnotation(PlayerOnly.class);

                data.addExecutor(exec.subCommand().toLowerCase(), new CommandExecutorData(
                        commandClass, method, perm != null ? perm.value() : null, po != null, exec.usage()
                ));
            }
        }

        if (!data.getExecutors().isEmpty()) {
            commands.put(name, data);
            registerLegacyCommand(name, data);
        }
    }

    private void registerLegacyCommand(String name, CommandData data) {
        if (commandMap == null) return;
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand cmd = constructor.newInstance(name, plugin);
            cmd.setDescription(data.getDescription());
            cmd.setUsage(data.getUsage());
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
            if (!data.getAliases().isEmpty()) cmd.setAliases(data.getAliases());
            commandMap.register(plugin.getDescription().getName().toLowerCase(), cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = label != null ? label.toLowerCase() : (command != null ? command.getName().toLowerCase() : "");
        CommandData data = commands.get(name);

        if (data == null) {
            data = commands.values().stream()
                    .filter(c -> c.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(name)))
                    .findFirst().orElse(null);
        }

        if (data == null) return false;

        if (data.getPermission() != null && !sender.hasPermission(data.getPermission())) {
            send(sender, "no_permission", null);
            return true;
        }

        String sub = args.length > 0 ? args[0].toLowerCase() : "";
        CommandExecutorData executor = data.getExecutor(sub);

        if (executor == null) {
            executor = data.getExecutor("");
            if (executor == null) {
                send(sender, "unknown_command", Map.of("{usage}", data.getUsage()));
                return true;
            }
        } else if (!sub.isEmpty()) {
            args = Arrays.copyOfRange(args, 1, args.length);
        }

        if (executor.isPlayerOnly() && !(sender instanceof Player)) {
            send(sender, "player_only", null);
            return true;
        }

        if (executor.getPermission() != null && !sender.hasPermission(executor.getPermission())) {
            send(sender, "no_permission", null);
            return true;
        }

        try {
            Method method = executor.getMethod();
            method.setAccessible(true);
            Object[] params = prepareParameters(method, sender, args);
            if (params == null) {
                send(sender, "unknown_command", Map.of("{usage}", executor.getUsage()));
                return true;
            }
            method.invoke(executor.getCommandInstance(), params);
        } catch (Exception e) {
            send(sender, "error", null);
            e.printStackTrace();
        }

        return true;
    }

    private void send(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender instanceof Player) {
            TextUtil.sendMessage((Player) sender, key, plugin, placeholders);
        } else {
            String text = plugin.getConfig().getString("messages." + key + ".text", 
                          plugin.getConfig().getString("messages." + key, ""));
            if (placeholders != null) {
                for (Map.Entry<String, String> e : placeholders.entrySet()) {
                    text = text.replace(e.getKey(), e.getValue());
                }
            }
            sender.sendMessage(text);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command != null ? command.getName().toLowerCase() : alias.toLowerCase();
        CommandData data = commands.get(name);

        if (data == null) {
            data = commands.values().stream()
                    .filter(c -> c.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(name)))
                    .findFirst().orElse(null);
        }

        if (data == null || (data.getPermission() != null && !sender.hasPermission(data.getPermission()))) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            final CommandData finalData = data;
            List<String> subs = data.getSubCommands().stream()
                    .filter(s -> !s.isEmpty())
                    .filter(s -> {
                        CommandExecutorData exec = finalData.getExecutor(s);
                        return exec.getPermission() == null || sender.hasPermission(exec.getPermission());
                    })
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

            if (subs.isEmpty() && data.getExecutor("") != null) {
                CommandExecutorData exec = data.getExecutor("");
                if (exec.getPermission() == null || sender.hasPermission(exec.getPermission())) {
                    return getTabCompletionForMethod(exec.getMethod(), sender, args, 0);
                }
            }
            return subs;
        }

        String sub = args[0].toLowerCase();
        CommandExecutorData executor = data.getExecutor(sub);
        if (executor == null) {
            executor = data.getExecutor("");
            if (executor != null && (executor.getPermission() == null || sender.hasPermission(executor.getPermission()))) {
                return getTabCompletionForMethod(executor.getMethod(), sender, args, 0);
            }
        } else if (executor.getPermission() == null || sender.hasPermission(executor.getPermission())) {
            return getTabCompletionForMethod(executor.getMethod(), sender, args, 1);
        }

        return new ArrayList<>();
    }

    private List<String> getTabCompletionForMethod(Method method, CommandSender sender, String[] args, int offset) {
        Parameter[] parameters = method.getParameters();
        int index = calculateParameterIndex(parameters, args.length - 1 - offset);
        if (index < 0 || index >= parameters.length) return new ArrayList<>();
        Parameter parameter = parameters[index];
        Class<?> type = parameter.getType();
        if (type == CommandSender.class || (type == Player.class && !parameter.isAnnotationPresent(Arg.class)) || type == String[].class) {
            return new ArrayList<>();
        }
        return getTabCompletionForParameter(parameter, args[args.length - 1]);
    }

    private int calculateParameterIndex(Parameter[] parameters, int pos) {
        int current = 0;
        for (int i = 0; i < parameters.length; i++) {
            Class<?> type = parameters[i].getType();
            if (type == CommandSender.class || (type == Player.class && !parameters[i].isAnnotationPresent(Arg.class)) || type == String[].class) {
                continue;
            }
            if (current == pos) return i;
            current++;
        }
        return -1;
    }

    private List<String> getTabCompletionForParameter(Parameter parameter, String input) {
        Arg arg = parameter.getAnnotation(Arg.class);
        if (arg != null && !arg.suggestions().isEmpty()) {
            return Arrays.stream(arg.suggestions().split(",")).map(String::trim)
                    .filter(v -> v.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
        }
        ArgumentResolver<?> resolver = argumentResolvers.get(parameter.getType());
        return resolver != null ? resolver.getDefaultSuggestions().stream()
                .filter(v -> v.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList()) : new ArrayList<>();
    }

    private Object[] prepareParameters(Method method, CommandSender sender, String[] args) {
        Parameter[] parameters = method.getParameters();
        Object[] params = new Object[parameters.length];
        int argIndex = 0;
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();
            if (type == CommandSender.class) {
                params[i] = sender;
            } else if (type == Player.class && !parameter.isAnnotationPresent(Arg.class)) {
                if (sender instanceof Player) params[i] = sender; else return null;
            } else if (type == String[].class) {
                params[i] = args;
            } else {
                if (argIndex >= args.length) {
                    if (isOptional(parameter)) params[i] = getDefault(type); else return null;
                } else {
                    Object parsed = parse(args[argIndex], type);
                    if (parsed == null && !isOptional(parameter)) return null;
                    params[i] = parsed;
                    argIndex++;
                }
            }
        }
        return params;
    }

    private boolean isOptional(Parameter p) { Arg a = p.getAnnotation(Arg.class); return a != null && a.optional(); }
    private Object parse(String arg, Class<?> type) { ArgumentResolver<?> r = argumentResolvers.get(type); return r != null ? r.resolve(arg) : null; }
    private Object getDefault(Class<?> type) {
        if (type == int.class || type == Integer.class) return 0;
        if (type == double.class || type == Double.class) return 0.0;
        if (type == boolean.class || type == Boolean.class) return false;
        if (type == String.class) return "";
        return null;
    }

    public interface ArgumentResolver<T> { T resolve(String input); List<String> getDefaultSuggestions(); }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) public @interface CommandInfo { String name(); String description() default ""; String usage() default ""; String[] aliases() default {}; String permission() default ""; }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface ExecuteCommand { String subCommand() default ""; String usage() default ""; }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface RequirePermission { String value(); }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface PlayerOnly {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER) public @interface Arg { String value() default ""; boolean optional() default false; String suggestions() default ""; }

    private static class CommandData {
        private final String name, description, usage, permission;
        private final List<String> aliases;
        private final Map<String, CommandExecutorData> executors = new HashMap<>();
        public CommandData(String name, String desc, String usage, String[] aliases, String perm) {
            this.name = name; this.description = desc.isEmpty() ? "zymis-cmd" : desc;
            this.usage = usage.isEmpty() ? "/" + name : usage; this.aliases = Arrays.asList(aliases);
            this.permission = perm.isEmpty() ? null : perm;
        }
        public void addExecutor(String sub, CommandExecutorData exec) { executors.put(sub, exec); }
        public CommandExecutorData getExecutor(String sub) { return executors.get(sub); }
        public Set<String> getSubCommands() { return executors.keySet(); }
        public Map<String, CommandExecutorData> getExecutors() { return executors; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getUsage() { return usage; }
        public List<String> getAliases() { return aliases; }
        public String getPermission() { return permission; }
    }

    private static class CommandExecutorData {
        private final Object instance;
        private final Method method;
        private final String permission;
        private final boolean playerOnly;
        private final String usage;
        public CommandExecutorData(Object inst, Method m, String p, boolean po, String u) {
            this.instance = inst; this.method = m; this.permission = p; this.playerOnly = po; this.usage = u;
        }
        public Object getCommandInstance() { return instance; }
        public Method getMethod() { return method; }
        public String getPermission() { return permission; }
        public boolean isPlayerOnly() { return playerOnly; }
        public String getUsage() { return usage; }
    }
}
