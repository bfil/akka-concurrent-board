$(function() {
	var username, joined = false, ws, lastClickedNote,
        labels = ['primary', 'success', 'info', 'warning', 'danger'];

	var board = $("#board"),
        trash = $("#trash"),
        usersContainer = $("#users-container"),
        connectedAs = $("#connected-as"),
        navBarToggler = $("#navbar-toggler"),
        loggedInDropdown = $("#logged-in-dropdown"),
        users = $("#users"),
		loginPanel = $("#board-login"),
        boardContainer = $("#board-container"),
		controlsPanel = $("#board-controls"),
		usernameInput = $("#username"),
		connectButton = $("#connect"),
		disconnectButton = $("#disconnect"),
		noteTextInput = $("#note-text"),
		addNoteButton = $("#add-note");

	function showAlert(s, type) {
        type = type || "info";
		var el = $("<div>" + s + "</div>").addClass("alert alert-" + type);
        el.appendTo("body");
        setTimeout(function() {
            el.fadeOut(500, function() {
                $(this).remove();
            });
        }, 1500);
	}

	function send(name, data) {
		if (ws) {
			ws.send(JSON.stringify({
				message : name,
				data : data
			}));
		}
	}
	
	function showLogin() {
        navBarToggler.hide();
        loggedInDropdown.hide();
		boardContainer.hide();
		loginPanel.show();
	}
	
	function showBoard() {
        navBarToggler.css("display", "");
        loggedInDropdown.show();
        connectedAs.text(username);
        boardContainer.css("display", "inline-block");
		loginPanel.hide();
	}
	
	function handleMessage(message, data) {
		switch (message) {
            case "Grabbed":
            	$(lastClickedNote).find("span.user").text(username);
                break;
            case "NotGrabbed":
                board.trigger("drop");
                $(lastClickedNote).removeClass("dragging").addClass("animated shake");
                setTimeout(function() {
                    $(lastClickedNote).removeClass("animated shake");
                }, 400);
                break;
			case "Joined":
				if (username === data.username) {
					joined = true;
					showBoard();
					showAlert("You joined the board as: " + username);
				} else
					showAlert(data.username + " joined the board");
				break;
			case "CannotJoin":
				showAlert("Cannot join the board: <br>" + data.error, "danger");
				ws.close();
				break;
			case "Quit":
				showAlert(data.username + " left the board");
				break;
			case "NoteAdded":
				var who = username === data.username ? "You" : data.username;
				showAlert(who + " added a note");
				break;
			case "BoardUpdate":
                var allNotes = $.map(board.find(".note"), function(note) {
                    return $(note).attr("id").replace("note-", "");
                });
                users.empty();
                for ( var i = 0; i < data.users.length; i++) {
                    var user = data.users[i];
                    (function(user) {
                        var userLabel = $("<span></span");
                        userLabel.addClass("label label-" + labels[i % labels.length]);
                        userLabel.text(user);
                        userLabel.attr("id", "user-" + user);
                        users.append(userLabel);
                    })(user);
                }
				for ( i = 0; i < data.notes.length; i++) {
                    var noteState = data.notes[i];
                    var note = $("#note-" + noteState.id);
                    (function(note) {
                        if(allNotes.indexOf(noteState.id.toString()) != -1) allNotes.splice(allNotes.indexOf(noteState.id.toString()), 1);
                        if(note.length) {
                            note.css({
                                left : noteState.x,
                                top : noteState.y,
                                "z-index": i
                            });
                            note.find("span.text").text(noteState.text);
                            note.find("span.user").text(noteState.owner || "").removeClass().addClass("user " + users.find("#user-" + noteState.owner).attr("class"));
                        } else {
                            note = $("<div></div>");
                            note.attr("id", "note-" + noteState.id);
                            note.data("id", noteState.id);
                            note.css({
                                left : noteState.x,
                                top : noteState.y,
                                "z-index": i
                            }).addClass("note");
                            var noteContent = $('<div class="content"></div>');
                            var text = $("<span></span>").addClass("text").text(noteState.text).keydown(function(e) {
                            	if(e.which === 13) {
                            		e.preventDefault();
                            		$(this).blur();
                            		send("EditNote", {
                                        noteId: $(this).parents(".note").data("id"),
                                        text: $(this).text()
                                    });
                            	}
                            });
                            var user = $("<span></span>").addClass("user").text(noteState.owner).addClass(users.find("#user-" + noteState.owner).attr("class"));
                            noteContent.append(text);
                            noteContent.append(user);
                            note.append(noteContent);
                            board.append(note);

                            note.attr("draggable", true);
                            note.on("dragstart", function(e) {
                                lastClickedNote = this;
                                send("GrabNote", {
                                    noteId: $(this).data("id")
                                });
                                note.addClass("dragging");
                                trash.addClass("active");
                                e.originalEvent.dataTransfer.setData('note', this.id);
                                e.originalEvent.dataTransfer.setData('offsetX', e.originalEvent.offsetX);
                                e.originalEvent.dataTransfer.setData('offsetY', e.originalEvent.offsetY);
                            });

                            note.on("dragend", function(e) {
                                $(this).removeClass("dragging");
                            });

                            note.on("click", function(e) {
                                lastClickedNote = this;
                                send("GrabNote", {
                                    noteId: $(this).data("id")
                                });
                            });
                        }
                        
                        note.find("span.text").attr("contenteditable", noteState.owner === username);
                    })(note);
                }
                for(i=0; i<allNotes.length; i++) {
                    var noteIdToDelete = "note-" + allNotes[i];
                    $("#" + noteIdToDelete).remove();
                }
				break;
		}
	}

    board.on("dragenter", function(e) {
        board.addClass("dragging");
    });

    board.on("dragleave", function(e) {
        board.removeClass("dragging");
    });

    board.on("dragover", function(e) {
        board.addClass("dragging");
        e.preventDefault();
    });

    board.on("drop", function(e) {
        e.stopPropagation();
        board.removeClass("dragging");
        trash.removeClass("dragging active");

        if(e.originalEvent) {
            var note = $("#" + e.originalEvent.dataTransfer.getData('note')),
                offsetX = e.originalEvent.dataTransfer.getData("offsetX"),
                offsetY = e.originalEvent.dataTransfer.getData("offsetY");
            
            var x = e.originalEvent.clientX - board.offset().left - offsetX,
                y = e.originalEvent.clientY - board.offset().top - offsetY;
            send("MoveNote", {
                noteId: note.data("id"),
                x: x,
                y: y
            });
        }
    });

    trash.on("dragenter", function(e) {
        trash.addClass("dragging");
    });

    trash.on("dragleave", function(e) {
        trash.removeClass("dragging");
    });

    trash.on("dragover", function(e) {
        trash.addClass("dragging");
        e.preventDefault();
    });

    trash.on("drop", function(e) {
        e.stopPropagation();
        
        board.removeClass("dragging");
        trash.removeClass("dragging active");
        trash.width();

        if(e.originalEvent) {
            var note = $("#" + e.originalEvent.dataTransfer.getData('note'));
            note.removeClass("dragging");
            var x = e.originalEvent.clientX - board.offset().left - (note.width() / 2),
                y = e.originalEvent.clientY - board.offset().top - (note.height() / 2);
            send("RemoveNote", {
                noteId: note.data("id")
            });
        }
    });

	addNoteButton.click(function(e) {
		var noteText = noteTextInput.val();
		if(noteText) {
			send("AddNote", {
				text: noteText
			});
			noteTextInput.val("");
		}
		e.preventDefault();
	});

	disconnectButton.click(function(e) {
		ws.close();
		e.preventDefault();
	});

	connectButton.click(function(e) {
		username = usernameInput.val();
		if (username) {
			ws = new WebSocket("ws://" + window.location.hostname + ":8125/");

			ws.onopen = function(ev) {
				send("Join", {
					username : username
				});
			};
			
			ws.onclose = function(ev) {
				if (joined) showAlert("You left the board");
				joined = false;
				showLogin();
				board.empty();
			};
			
			ws.onmessage = function(ev) {
				var messageObject = JSON.parse(ev.data);
				handleMessage(messageObject.message, messageObject.data);
			};
			
			ws.onerror = function(ev) {
				console.error(ev);
				showAlert("Unable to connect to the board", "danger");
			};
		}

		e.preventDefault();
	});

    navBarToggler.hide();
});