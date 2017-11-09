/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.commandmeta;

import fredboat.commandmeta.abs.Command;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandRegistry {

    private static Map<Module, CommandRegistry> modules = new HashMap<>();

    public static void registerModule(@Nonnull CommandRegistry registry) {
        modules.put(registry.module, registry);
    }

    @Nullable
    public static Command findCommand(@Nonnull String name) {
        return modules.values().stream()
                .map(cr -> cr.getCommand(name))
                .findAny()
                .orElse(null);
    }

    public static int getTotalSize() {
        return modules.values().stream()
                .mapToInt(CommandRegistry::getSize)
                .sum();
    }

    public static Set<String> getAllRegisteredCommandsAndAliases() {
        return modules.values().stream()
                .flatMap(cr -> cr.getRegisteredCommandsAndAliases().stream())
                .collect(Collectors.toSet());
    }


    private HashMap<String, Command> registry = new HashMap<>();
    private final Module module;

    public CommandRegistry(@Nonnull Module module) {
        this.module = module;
        registerModule(this);
    }

    public void registerCommand(@Nonnull Command command) {
        String name = command.name.toLowerCase();
        registry.put(name, command);
        for (String alias : command.aliases) {
            registry.put(alias.toLowerCase(), command);
        }
    }

    @Nonnull
    public Set<String> getRegisteredCommandsAndAliases() {
        return registry.keySet();
    }

    public int getSize() {
        return registry.size();
    }

    @Nullable
    public Command getCommand(@Nonnull String name) {
        return registry.get(name);
    }

    public enum Module {
        ADMIN,
        INFORMATIONAL,
        CONFIG,
        MUSIC,
        MODERATION,
        UTILITY,
        FUN
    }
}
