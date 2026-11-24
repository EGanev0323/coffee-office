/*
  Цветът на маркера за всеки колега: един от шестте, избран по id-то ((id − 1) % 6),
  така че първият профил е синият и цветът на всеки колега е един и същ навсякъде
  (имена и чертички на дъската, аватари, баланси в таблиците). Класовете m-blue … m-brown
  задават CSS променливата --m, която четат .board-name, .tally-stroke, .avatar, .balance-cell.
*/
const CLASSES = ['m-blue', 'm-red', 'm-green', 'm-purple', 'm-orange', 'm-brown']

/** Класът на маркера по id на колегата. Без id → синият (системният глас). */
export function markerClass(userId) {
  if (userId == null) return 'm-blue'
  return CLASSES[(((userId - 1) % 6) + 6) % 6]
}
