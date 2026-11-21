async function sendPostRequest(url, payload, callback) {
    payload["X-XSRF-TOKEN"] = Wiki.CsrfProtection;
    try {
        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: payload
        });
        callback(response);

    } catch (error) {
        callback(error);
    }
}

function UserManagementPluginLockuser(btnid, otherbtnid, username) {
    sendPostRequest('/search/suggestions/lock', [username], function (result) {
        if (!result) {
            //operation failed, show a message?
            alert(result);
        } else {
            document.getElementById(btnid).setAttribute("disabled", true);
            document.getElementById(otherbtnid).removeAttribute("disabled");
        }
    });
}
function UserManagementPluginUnlockUser(btnid, otherbtnid, username) {
    sendPostRequest('/admin-user-management/unlock', [username], function (result) {
        if (!result) {
            //operation failed, show a message?
            alert(result);
        } else {
            document.getElementById(btnid).setAttribute("disabled", true);
            document.getElementById(otherbtnid).removeAttribute("disabled");
        }
    });
}
function UserManagementPluginExpirePassword(btnid, username) {
    sendPostRequest('/admin-user-management/expire', [username], function (result) {
        if (!result) {
            //operation failed, show a message?
            alert(result);
        } else {
            document.getElementById(btnid).setAttribute("disabled", true);
        }
    });
}
function UserManagementPluginDeleteUser(rowid, username) {
    sendPostRequest('/admin-user-management/delete', [username], function (result) {
        if (!result) {
            //operation failed, show a message?
            alert(result);
        } else {
            document.getElementById(rowid).remove();
        }
    });
}
