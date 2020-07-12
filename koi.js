class Koi {
    constructor(address = "wss://live.casterlabs.co/koi") {
        this.address = address;
        this.listeners = {};
        this.reconnect();
    }

    addEventListener(type, callback) {
        type = type.toLowerCase();

        let callbacks = this.listeners[type.toLowerCase()];

        if (!callbacks) callbacks = [];

        callbacks.push(callback);

        this.listeners[type.toLowerCase()] = callbacks;
    }

    broadcast(type, data) {
        let listeners = this.listeners[type.toLowerCase()];

        if (listeners) {
            listeners.forEach((callback) => {
                try {
                    callback(data);
                } catch (e) {
                    console.error("An event listener produced an exception: ");
                    console.error(e);
                }
            });
        }
    }

    reconnect() {
        if (this.ws && !this.ws.CLOSED) {
            this.ws.close();
        }

        let instance = this;
        this.ws = new WebSocket(this.address);

        this.ws.onopen = function () {
            instance.broadcast("open");
        };

        this.ws.onclose = function () {
            instance.broadcast("close");
        };

        this.ws.onmessage = function (message) {
            var raw = message.data;
            var json = JSON.parse(raw);

            if (json["type"] == "KEEP_ALIVE") {
                var json = {
                    request: "KEEP_ALIVE"
                };

                this.send(JSON.stringify(json));
            } else if (json["type"] == "EVENT") {
                var event = json["event"];

                instance.broadcast("event", event);
                instance.broadcast(event["event_type"], event);
            } else {
                instance.broadcast(json["type"], json);
            }
        };
    }

    isAlive() {
        return this.ws.readyState == this.ws.OPEN;
    }

    addUser(user) {
        var json = {
            request: "ADD",
            user: user
        };

        this.ws.send(JSON.stringify(json));
    }

    test(user, event) {
        var json = {
            request: "TEST",
            test: event,
            user: user
        };

        this.ws.send(JSON.stringify(json));
    }

    removeUser(user) {
        var json = {
            request: "REMOVE",
            user: user
        };

        this.ws.send(JSON.stringify(json));
    }

    close() {
        this.ws.close();
    }

}
