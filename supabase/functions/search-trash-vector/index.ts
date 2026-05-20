import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type, sigungu-code',
}

serve(async (req) => {
  // CORS Preflight 처리
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const sigunguCode = req.headers.get("sigungu-code") || "1100000000"; // 디폴트 시군구 코드 (서울시 등)
    
    // 1. 요청 바디에서 Raw Image Binary 데이터 읽기
    const arrayBuffer = await req.arrayBuffer();
    if (arrayBuffer.byteLength === 0) {
      return new Response(JSON.stringify({ error: "이미지 바이트 데이터가 비어 있습니다." }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    const imageBytes = new Uint8Array(arrayBuffer);

    // 2. HuggingFace Inference API를 통해 이미지 임베딩(512차원) 생성
    // API 키는 Supabase Dashboard의 Secrets에 HUGGINGFACE_API_KEY 로 등록하여 사용합니다.
    const hfApiKey = Deno.env.get("HUGGINGFACE_API_KEY");
    if (!hfApiKey) {
      return new Response(JSON.stringify({ error: "백엔드 HUGGINGFACE_API_KEY 설정이 누락되었습니다." }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    console.log(`[Info] HuggingFace CLIP API 호출 중 (이미지 크기: ${imageBytes.length} bytes)...`);
    
    // HuggingFace Feature Extraction 파이프라인 호출
    const hfResponse = await fetch(
      "https://api-inference.huggingface.co/pipeline/feature-extraction/openai/clip-vit-base-patch32",
      {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${hfApiKey}`,
          "Content-Type": "application/octet-stream"
        },
        body: imageBytes
      }
    );

    if (!hfResponse.ok) {
      const errText = await hfResponse.text();
      console.error("[Error] HuggingFace API 오류:", errText);
      return new Response(JSON.stringify({ error: `HuggingFace 임베딩 추출 실패: ${errText}` }), {
        status: 502,
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    // HuggingFace Feature Extraction API는 [512] 혹은 [[512]] 형태로 텍스트/이미지 벡터를 반환합니다.
    const embeddingResult = await hfResponse.json();
    let embedding: number[] = [];

    if (Array.isArray(embeddingResult)) {
      if (Array.isArray(embeddingResult[0])) {
        embedding = embeddingResult[0] as number[];
      } else {
        embedding = embeddingResult as number[];
      }
    }

    if (embedding.length !== 512) {
      return new Response(JSON.stringify({ error: `올바르지 않은 임베딩 차원입니다 (차원 수: ${embedding.length}, 필요 개수: 512)` }), {
        status: 502,
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    // 3. Supabase DB 클라이언트 생성 및 RPC 호출
    const supabaseUrl = Deno.env.get("SUPABASE_URL") || "";
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";

    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    console.log(`[Info] RPC match_waste_items 호출 중 (sigungu_code: ${sigunguCode})...`);

    const { data: matchResults, error: rpcError } = await supabase.rpc("match_waste_items", {
      query_embedding: embedding,
      match_threshold: 0.1, // 기본 코사인 유사도 최소 기준값 (임계값 완화하여 폭넓게 매칭)
      match_count: 5,        // 후보 5개 수집
      user_sigungu_code: sigunguCode
    });

    if (rpcError) {
      console.error("[Error] RPC 호출 실패:", rpcError);
      return new Response(JSON.stringify({ error: `데이터베이스 조회 실패: ${rpcError.message}` }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    console.log(`[Success] 매칭 성공! 결과 개수: ${matchResults?.length || 0}`);

    return new Response(JSON.stringify({ results: matchResults }), {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });

  } catch (err) {
    console.error("[Error] Edge Function 에러:", err);
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
  }
})
