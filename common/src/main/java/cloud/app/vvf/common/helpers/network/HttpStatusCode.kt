package cloud.app.vvf.common.helpers.network

import androidx.annotation.RawRes
import cloud.app.vvf.common.R

class HttpReason(@RawRes val code: Int) : Throwable()

fun getHttpStatusRes(code: Int): Int {
  return when (code) {
    100 -> R.string._100
    101 -> R.string._101
    102 -> R.string._102
    200 -> R.string._200
    201 -> R.string._201
    202 -> R.string._202
    203 -> R.string._203
    204 -> R.string._204
    205 -> R.string._205
    206 -> R.string._206
    207 -> R.string._207
    208 -> R.string._208
    226 -> R.string._226
    300 -> R.string._300
    301 -> R.string._301
    302 -> R.string._302
    303 -> R.string._303
    304 -> R.string._304
    305 -> R.string._305
    307 -> R.string._307
    308 -> R.string._308
    400 -> R.string._400
    401 -> R.string._401
    402 -> R.string._402
    403 -> R.string._403
    404 -> R.string._404
    405 -> R.string._405
    406 -> R.string._406
    407 -> R.string._407
    408 -> R.string._408
    409 -> R.string._409
    410 -> R.string._410
    411 -> R.string._411
    412 -> R.string._412
    413 -> R.string._413
    414 -> R.string._414
    415 -> R.string._415
    416 -> R.string._416
    417 -> R.string._417
    418 -> R.string._418
    421 -> R.string._421
    422 -> R.string._422
    423 -> R.string._423
    424 -> R.string._424
    426 -> R.string._426
    428 -> R.string._428
    429 -> R.string._429
    431 -> R.string._431
    444 -> R.string._444
    451 -> R.string._451
    499 -> R.string._499
    500 -> R.string._500
    501 -> R.string._501
    502 -> R.string._502
    503 -> R.string._503
    504 -> R.string._504
    505 -> R.string._505
    506 -> R.string._506
    507 -> R.string._507
    508 -> R.string._508
    510 -> R.string._510
    511 -> R.string._511
    599 -> R.string._599
    else -> R.string._unknow
  }
}
