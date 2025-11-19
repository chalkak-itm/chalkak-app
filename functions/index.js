const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { OpenAI } = require("openai");
const functions = require("firebase-functions");

initializeApp();
const db = getFirestore();

exports.getWordData = onCall({ region: "asia-northeast3" }, async (request) => {
    // 1. Validate Input
    const word = request.data.word;
    if (!word) {
        throw new HttpsError('invalid-argument', 'No word provided');
    }

    const apiKey = functions.config().openai.key;
    const openai = new OpenAI({ apiKey: apiKey });

    const lowerWord = word.toLowerCase();
    const docRef = db.collection("words").doc(lowerWord);

    // 2. Check Firestore Cache First
    const docSnap = await docRef.get();
    if (docSnap.exists()) {
        return docSnap.data(); // Cache Hit!
    }

    // 3. Cache Miss: Ask GPT
    try {
        const completion = await openai.chat.completions.create({
            model: "gpt-4o-mini",
            messages: [
                {
                    role: "system",
                    content: `You are a helper for a kids' English app. 
                    Output MUST be in strict JSON format.
                    Structure: { "originalWord": "${word}", "meaning": "Korean meaning", "examples": [{ "sentence": "Eng sentence", "translation": "Kor translation" }] }
                    Constraint: Provide exactly 10 examples. Level: Elementary school.
                    ERROR HANDLING: If input is invalid or typo, output ONLY: { "error": "NOT_FOUND" }`
                },
                { role: "user", content: word }
            ],
            response_format: { type: "json_object" }
        });

        const content = completion.choices[0].message.content;
        const result = JSON.parse(content);

        // 4. Handle Invalid Word
        if (result.error === "NOT_FOUND") {
             return { isError: true, message: "Invalid word" };
        }

        // 5. Save to Firestore (Cache) & Return
        result.createdAt = new Date();
        await docRef.set(result);

        return result;

    } catch (error) {
        console.error("GPT Error:", error);
        throw new HttpsError('internal', 'GPT call failed');
    }
});