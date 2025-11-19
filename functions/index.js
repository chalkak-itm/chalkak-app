// index.js
import { onCall } from 'firebase-functions/v2/https';
import { OpenAI } from 'openai';
import * as logger from 'firebase-functions/logger';

/**
 * 단어의 한국어 의미와 예문을 GPT로부터 가져오는 Cloud Function
 * 
 * @param {Object} data - 요청 데이터
 * @param {string} data.word - 조회할 영어 단어
 * @returns {Object} - originalWord, meaning, examples를 포함한 객체
 */
export const getWordData = onCall(
  {
    region: 'asia-northeast3',
  },
  async (request) => {
    // OpenAI 클라이언트 초기화 (환경 변수에서 API 키 가져오기)
    // 환경 변수는 Firebase Console > Functions > Configuration에서 설정
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

      // GPT API 호출 - 3개 예문 생성
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

      // JSON 파싱 시도
      let parsedResponse;
      try {
        // JSON 코드 블록이 있는 경우 제거
        const cleanedText = responseText.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim();
        parsedResponse = JSON.parse(cleanedText);
      } catch (parseError) {
        logger.error('Failed to parse GPT response:', parseError);
        // 파싱 실패 시 기본 응답 (1개 예문)
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
      // examples 배열이 있는지 확인, 없으면 기본값
      let examples = parsedResponse.examples || [];
      
      // examples가 배열이 아니거나 비어있으면 기본 예문 생성
      if (!Array.isArray(examples) || examples.length === 0) {
        examples = [
          {
            sentence: `This is ${word}.`,
            translation: `이것은 ${word}입니다.`,
          },
        ];
      }

      // 최대 3개로 제한
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