// index.js
import { onCall } from 'firebase-functions/v2/https';
import { OpenAI } from 'openai';
import * as logger from 'firebase-functions/logger';

/**
 * Cloud Function that fetches Korean meanings and example sentences for a word from GPT
 * 
 * @param {Object} data - Request payload
 * @param {string} data.word - English word to look up
 * @returns {Object} - Object containing originalWord, meaning, and examples
 */
export const getWordData = onCall(
  {
    region: 'asia-northeast3',
  },
  async (request) => {
    // Initialize OpenAI client (read API key from environment variables)
    // Environment variables are configured in Firebase Console > Functions > Configuration
    const apiKey = process.env.OPENAI_API_KEY;
    
    if (!apiKey) {
      logger.error('OPENAI_API_KEY is not set');
      return {
        isError: true,
        error: 'OpenAI API 키가 설정되지 않았습니다.',
      };
    }

    const openai = new OpenAI({
      apiKey: apiKey,
    });

    try {
      const word = request.data?.word;
      
      if (!word || typeof word !== 'string') {
        logger.error('Invalid word parameter:', word);
        return {
          isError: true,
          error: '단어가 제공되지 않았습니다.',
        };
      }

      logger.info('Fetching word data for:', word);

      // Call GPT API - generate 3 example sentences
      const completion = await openai.chat.completions.create({
        model: 'gpt-3.5-turbo',
        messages: [
          {
            role: 'system',
            content: 'You are a helpful English-Korean dictionary assistant. Provide Korean translations and example sentences in JSON format.',
          },
          {
            role: 'user',
            content: `Please provide the Korean meaning and exactly 3 example sentences for the English word "${word}". 
                     Respond in JSON format: {"meaning": "한국어 의미", "examples": [{"sentence": "English example sentence 1", "translation": "한국어 번역 1"}, {"sentence": "English example sentence 2", "translation": "한국어 번역 2"}, {"sentence": "English example sentence 3", "translation": "한국어 번역 3"}]}`,
          },
        ],
        temperature: 0.7,
        max_tokens: 500,
      });

      const responseText = completion.choices[0]?.message?.content || '';
      logger.info('GPT Response:', responseText);

      // Try to parse JSON
      let parsedResponse;
      try {
        // Remove JSON code block if present
        const cleanedText = responseText.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim();
        parsedResponse = JSON.parse(cleanedText);
      } catch (parseError) {
        logger.error('Failed to parse GPT response:', parseError);
        // Fallback response when parsing fails (single example)
        parsedResponse = {
          meaning: `${word}의 의미를 찾을 수 없습니다.`,
          examples: [
            {
              sentence: `This is ${word}.`,
              translation: `이것은 ${word}입니다.`,
            },
          ],
        };
      }

      const meaning = parsedResponse.meaning || `${word}의 의미`;
      // Ensure examples array exists, otherwise use defaults
      let examples = parsedResponse.examples || [];
      
      // If examples is not an array or empty, generate default example
      if (!Array.isArray(examples) || examples.length === 0) {
        examples = [
          {
            sentence: `This is ${word}.`,
            translation: `이것은 ${word}입니다.`,
          },
        ];
      }

      // Limit to maximum of 3
      if (examples.length > 3) {
        examples = examples.slice(0, 3);
      }

      return {
        isError: false,
        originalWord: word,
        meaning: meaning,
        examples: examples,
      };
    } catch (error) {
      logger.error('Error in getWordData:', error);
      return {
        isError: true,
        error: error.message || '단어 데이터를 가져오는 중 오류가 발생했습니다.',
      };
    }
  }
);
