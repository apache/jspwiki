package org.apache.wiki.ui;

import org.apache.wiki.api.core.Command;


/**
 * Placeholder class for all Commands.
 */
public interface AllCommands {

    /**
     * Returns a defensively-created array of all static Commands.
     *
     * @return the array of commands
     */
    static Command[] get() {
        return new Command[] {
            GroupCommand.DELETE_GROUP,
            GroupCommand.EDIT_GROUP,
            GroupCommand.VIEW_GROUP,
            PageCommand.ATTACH,
            PageCommand.COMMENT,
            PageCommand.CONFLICT,
            PageCommand.DELETE,
            PageCommand.DIFF,
            PageCommand.EDIT,
            PageCommand.INFO,
            PageCommand.NONE,
            PageCommand.OTHER,
            PageCommand.PREVIEW,
            PageCommand.RENAME,
            PageCommand.RSS,
            PageCommand.UPLOAD,
            PageCommand.VIEW,
            RedirectCommand.REDIRECT,
            WikiCommand.CREATE_GROUP,
            WikiCommand.ERROR,
            WikiCommand.FIND,
            WikiCommand.INSTALL,
            WikiCommand.LOGIN,
            WikiCommand.LOGOUT,
            WikiCommand.MESSAGE,
            WikiCommand.PREFS,
            WikiCommand.WORKFLOW,
            WikiCommand.ADMIN
        };
    }

}
