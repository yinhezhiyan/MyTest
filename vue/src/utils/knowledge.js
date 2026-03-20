const INVALID_KNOWLEDGE_POINT_PATTERN = /(question-?txt导入|txt导入|ocr导入|图片ocr导入|导入标记|导入来源)/i

export const sanitizeKnowledgePoints = (rawPoints) => {
  const source = Array.isArray(rawPoints)
    ? rawPoints
    : String(rawPoints || '')
        .split(/[，,]/)

  const seen = new Set()
  return source
    .map(item => String(item || '').replace(/\s+/g, ' ').trim())
    .filter(item => item && !INVALID_KNOWLEDGE_POINT_PATTERN.test(item))
    .filter(item => {
      if (seen.has(item)) return false
      seen.add(item)
      return true
    })
}

export const parseKnowledgePoints = (value) => {
  if (Array.isArray(value)) return sanitizeKnowledgePoints(value)
  try {
    return sanitizeKnowledgePoints(JSON.parse(value || '[]'))
  } catch (e) {
    return sanitizeKnowledgePoints(value)
  }
}
