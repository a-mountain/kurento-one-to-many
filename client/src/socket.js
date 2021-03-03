const CALL_ENDPOINT = 'ws://localhost:8443/call';
let ws;
const OPEN = 1;

let endpointHandlers;

const sendMessage = (type, content) => {
    if (ws.readyState === OPEN) {
        const message = {type, content};
        const jsonMessage = JSON.stringify(message)
        ws.send(jsonMessage);
        console.info(`Send message: ${jsonMessage}`)
    }
};

const setOnMessage = (onMessage) => {
    ws.onmessage = event => {
        const parsedData = JSON.parse(event.data)
        const {type, content} = parsedData;
        console.info(`WebSocket message received ${JSON.stringify(parsedData)}`)
        onMessage(type, content);
    };
};

const setHandlers = handlers => {
    endpointHandlers = handlers;
}

const onMessage = async (type, message) => {
    const handler = endpointHandlers[type];
    if (handler) {
        handler(message);
        return;
    }
    console.error(`Unrecognized message type: ${JSON.stringify(message)}`)
};

const initSocket = () => {
    ws = new WebSocket(CALL_ENDPOINT);
    ws.onerror = (event) => {
        console.error(`WebSocket error observed: ${JSON.stringify(event)}`);
    };
    ws.onopen = () => {
        console.info(`WebSocket is open: ${CALL_ENDPOINT}`);
    };
    ws.onclose = () => {
        console.info('WebSocket is closed');
    };
    setOnMessage(onMessage);
}

export {sendMessage, setHandlers, initSocket}
