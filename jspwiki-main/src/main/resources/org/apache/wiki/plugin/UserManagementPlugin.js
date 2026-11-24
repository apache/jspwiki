/**
 * 
 * @param {type} url
 * @param {type} payload an array of values
 * @param {type} callback
 * @returns {undefined}
 */
async function sendPostRequest(url, payload, callback) {
    let msg = {};
    msg.params = payload.join(",");
    msg["X-XSRF-TOKEN"] = window.Wiki.CsrfProtection;
    try {
        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams(msg)
        });
        callback(response);

    } catch (error) {
        callback(error);
    }
}

async function UserManagementPluginLockuser(btnid, otherbtnid, username) {
    sendPostRequest('ajax/admin-user-management/lock', [username], async function (result) {
        if (!result.ok) {
            //operation failed, show a message?
            const data = await result.text();
             alert(result.status + " " + data);
        } else {
            document.getElementById(btnid).setAttribute("disabled", true);
            document.getElementById(otherbtnid).removeAttribute("disabled");
        }
    });
}
async function UserManagementPluginUnlockUser(btnid, otherbtnid, username) {
    sendPostRequest('ajax/admin-user-management/unlock', [username], async function (result) {
        if (!result.ok) {
            //operation failed, show a message?
             const data = await result.text();
             alert(result.status + " " + data);
        } else {
            document.getElementById(btnid).setAttribute("disabled", true);
            document.getElementById(otherbtnid).removeAttribute("disabled");
        }
    });
}
async function UserManagementPluginExpireAll() {
    sendPostRequest('ajax/admin-user-management/expireAll', [], async function (result) {
        if (!result.ok) {
            //operation failed, show a message?
           const data = await result.text();
             alert(result.status + " " + data);
        } else {
             alert("success");
        }
    });
}
async function UserManagementPluginExpirePassword(btnid, username) {
    sendPostRequest('ajax/admin-user-management/expire', [username], async function (result) {
        if (!result.ok) {
            //operation failed, show a message?
           const data = await result.text();
             alert(result.status + " " + data);
        } else {
            document.getElementById(btnid).setAttribute("disabled", true);
        }
    });
}
function UserManagementPluginDeleteUser(rowid, username) {
    sendPostRequest('ajax/admin-user-management/delete', [username], async function (result) {
        if (!result.ok) {
            //operation failed, show a message?
            const data = await result.text();
             alert(result.status + " " + data);
        } else {
            document.getElementById(rowid).remove();
        }
    });
}
