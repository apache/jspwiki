async function sendPostRequest(url, payload, callback) {
    payload["X-XSRF-TOKEN"] = Wiki.CsrfProtection;
    try {
        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            data: payload
        });
        callback(response);

    } catch (error) {
        callback(error);
    }
}

function UserManagementPluginLockuser(btnid, otherbtnid, username) {
    sendPostRequest('ajax/admin-user-management/lock', {"username": username}, function (result) {
        if (!result.ok) {
            //operation failed, show a message?
            alert(result);
        } else {
            document.getElementById(btnid).setAttribute("disabled", true);
            document.getElementById(otherbtnid).removeAttribute("disabled");
        }
    });
}
function UserManagementPluginUnlockUser(btnid, otherbtnid, username) {
    sendPostRequest('ajax/admin-user-management/unlock', {"username": username}, function (result) {
        if (!result.ok) {
            //operation failed, show a message?
            alert(result);
        } else {
            document.getElementById(btnid).setAttribute("disabled", true);
            document.getElementById(otherbtnid).removeAttribute("disabled");
        }
    });
}
function UserManagementPluginExpirePassword(btnid, username) {
    sendPostRequest('ajax/admin-user-management/expire', {"username": username}, function (result) {
        if (!result.ok) {
            //operation failed, show a message?
            alert(result);
        } else {
            document.getElementById(btnid).setAttribute("disabled", true);
        }
    });
}
function UserManagementPluginDeleteUser(rowid, username) {
    sendPostRequest('ajax/admin-user-management/delete', {"username": username}, function (result) {
        if (!result.ok) {
            //operation failed, show a message?
            alert(result);
        } else {
            document.getElementById(rowid).remove();
        }
    });
}
