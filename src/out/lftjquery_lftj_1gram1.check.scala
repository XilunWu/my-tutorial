/*****************************************
Emitting Generated Code
*******************************************/
class Snippet extends ((java.lang.String)=>(Unit)) {
  def apply(x0:java.lang.String): Unit = {
    val x1 = println("Phrase,Year,MatchCount,VolumeCount")
    var x2: Int = 0
    val x3 = new Array[java.lang.String](4194304)
    val x4 = new Array[java.lang.String](4194304)
    val x5 = new Array[java.lang.String](4194304)
    val x6 = new Array[java.lang.String](4194304)
    val x7 = new Array[java.lang.String](4194304)
    val x8 = new Array[java.lang.String](4194304)
    val x9 = new Array[java.lang.String](4194304)
    val x10 = new Array[java.lang.String](4194304)
    val x11 = new Array[Int](4194304)
    val x12 = new Array[Int](4194304)
    val x13 = new Array[Int](4194304)
    val x14 = new Array[Int](4194304)
    val x15 = new Array[Int](4)
    val x16 = new Array[Boolean](4)
    var x17: Int = 0
    val x18 = x16(0) = true
    val x19 = x17 += 1
    val x20 = x17
    val x21 = x16(x20) = true
    val x22 = x17 += 1
    val x23 = x17
    val x24 = x16(x23) = true
    val x25 = x17 += 1
    val x26 = x17
    val x27 = x16(x26) = true
    val x28 = x17 += 1
    val x29 = new Array[Int](4)
    var x30: Int = 0
    val x31 = new scala.lms.tutorial.Scanner("src/data/1gram.csv")
    val x45 = while ({val x32 = x31.hasNext
      x32}) {
      val x34 = x31.next('\t')
      val x35 = x31.next('\t')
      val x36 = x31.next('\t')
      val x37 = x31.next('\n')
      val x38 = x2
      val x39 = x3(x38) = x34
      val x40 = x4(x38) = x35
      val x41 = x5(x38) = x36
      val x42 = x6(x38) = x37
      val x43 = x2 += 1
      ()
    }
    val x46 = x31.close
    var x47: Int = 0
    val x48 = new Array[java.lang.String](4194304)
    val x49 = new Array[java.lang.String](4194304)
    val x50 = new Array[java.lang.String](4194304)
    val x51 = new Array[java.lang.String](4194304)
    val x52 = new Array[java.lang.String](4194304)
    val x53 = new Array[java.lang.String](4194304)
    val x54 = new Array[java.lang.String](4194304)
    val x55 = new Array[java.lang.String](4194304)
    val x56 = new Array[Int](4194304)
    val x57 = new Array[Int](4194304)
    val x58 = new Array[Int](4194304)
    val x59 = new Array[Int](4194304)
    val x60 = new Array[Int](4)
    val x61 = new Array[Boolean](4)
    var x62: Int = 0
    val x63 = x61(0) = true
    val x64 = x62 += 1
    val x65 = x62
    val x66 = x61(x65) = true
    val x67 = x62 += 1
    val x68 = x62
    val x69 = x61(x68) = true
    val x70 = x62 += 1
    val x71 = x62
    val x72 = x61(x71) = true
    val x73 = x62 += 1
    val x74 = new Array[Int](4)
    var x75: Int = 0
    val x76 = new scala.lms.tutorial.Scanner("src/data/1gram.csv")
    val x90 = while ({val x77 = x76.hasNext
      x77}) {
      val x79 = x76.next('\t')
      val x80 = x76.next('\t')
      val x81 = x76.next('\t')
      val x82 = x76.next('\n')
      val x83 = x47
      val x84 = x48(x83) = x79
      val x85 = x49(x83) = x80
      val x86 = x50(x83) = x81
      val x87 = x51(x83) = x82
      val x88 = x47 += 1
      ()
    }
    val x91 = x76.close
    val x92 = new Array[java.lang.String](4)
    val x94 = x92.foreach{
      x93 =>
      ()
    }
    val x95 = new Array[Int](4)
    var x96: Int = 0
    var x97: Int = 0
    var x98: Boolean = false
    val x208 = while ({val x99 = x96
      val x100 = x2
      val x101 = x99 < x100
      x101}) {
      x98 = false
      val x204 = while ({val x104 = x97
        val x105 = x104 < 4
        x105}) {
        val x107 = x97
        val x108 = x107 == 0
        val x201 = if (x108) {
          val x109 = x96
          val x110 = x3(x109)
          val x111 = x98
          val x112 = x92(0)
          val x113 = x112 != x110
          val x114 = x111 || x113
          val x124 = if (x114) {
            val x115 = x95(0)
            val x116 = x7(x115) = x110
            val x117 = x95(1)
            val x118 = x11(x115) = x117
            val x119 = x92(0) = x110
            val x120 = x115 + 1
            val x121 = x95(0) = x120
            x98 = true
            ()
          } else {
            ()
          }
          x124
        } else {
          val x126 = x107 == 1
          val x199 = if (x126) {
            val x127 = x96
            val x128 = x4(x127)
            val x129 = x98
            val x130 = x92(1)
            val x131 = x130 != x128
            val x132 = x129 || x131
            val x142 = if (x132) {
              val x133 = x95(1)
              val x134 = x8(x133) = x128
              val x135 = x95(2)
              val x136 = x12(x133) = x135
              val x137 = x92(1) = x128
              val x138 = x133 + 1
              val x139 = x95(1) = x138
              x98 = true
              ()
            } else {
              ()
            }
            x142
          } else {
            val x144 = x107 == 2
            val x197 = if (x144) {
              val x145 = x96
              val x146 = x5(x145)
              val x147 = x98
              val x148 = x92(2)
              val x149 = x148 != x146
              val x150 = x147 || x149
              val x160 = if (x150) {
                val x151 = x95(2)
                val x152 = x9(x151) = x146
                val x153 = x95(3)
                val x154 = x13(x151) = x153
                val x155 = x92(2) = x146
                val x156 = x151 + 1
                val x157 = x95(2) = x156
                x98 = true
                ()
              } else {
                ()
              }
              x160
            } else {
              val x162 = x107 == 3
              val x195 = if (x162) {
                val x163 = x96
                val x164 = x6(x163)
                val x165 = x98
                val x166 = x92(3)
                val x167 = x166 != x164
                val x168 = x165 || x167
                val x176 = if (x168) {
                  val x169 = x95(3)
                  val x170 = x10(x169) = x164
                  val x171 = x92(3) = x164
                  val x172 = x169 + 1
                  val x173 = x95(3) = x172
                  x98 = true
                  ()
                } else {
                  ()
                }
                x176
              } else {
                val x178 = x96
                val x179 = x3(x178)
                val x180 = x98
                val x181 = x92(0)
                val x182 = x181 != x179
                val x183 = x180 || x182
                val x193 = if (x183) {
                  val x184 = x95(0)
                  val x185 = x7(x184) = x179
                  val x186 = x95(1)
                  val x187 = x11(x184) = x186
                  val x188 = x92(0) = x179
                  val x189 = x184 + 1
                  val x190 = x95(0) = x189
                  x98 = true
                  ()
                } else {
                  ()
                }
                x193
              }
              x195
            }
            x197
          }
          x199
        }
        val x202 = x97 += 1
        ()
      }
      val x205 = x96 += 1
      x97 = 0
      ()
    }
    val x240 = while ({val x209 = x97
      val x210 = x209 < 3
      x210}) {
      val x212 = x97
      val x213 = x95(x212)
      val x214 = x212 + 1
      val x215 = x95(x214)
      val x216 = x212 == 0
      val x236 = if (x216) {
        val x217 = x11(x213) = x215
        x217
      } else {
        val x219 = x212 == 1
        val x234 = if (x219) {
          val x220 = x12(x213) = x215
          x220
        } else {
          val x222 = x212 == 2
          val x232 = if (x222) {
            val x223 = x13(x213) = x215
            x223
          } else {
            val x225 = x212 == 3
            val x230 = if (x225) {
              val x226 = x14(x213) = x215
              x226
            } else {
              val x228 = x11(x213) = x215
              x228
            }
            x230
          }
          x232
        }
        x234
      }
      val x237 = x15(x212) = x213
      val x238 = x97 += 1
      ()
    }
    val x241 = new Array[java.lang.String](4)
    val x243 = x241.foreach{
      x242 =>
      ()
    }
    val x244 = new Array[Int](4)
    var x245: Int = 0
    var x246: Int = 0
    var x247: Boolean = false
    val x357 = while ({val x248 = x245
      val x249 = x47
      val x250 = x248 < x249
      x250}) {
      x247 = false
      val x353 = while ({val x253 = x246
        val x254 = x253 < 4
        x254}) {
        val x256 = x246
        val x257 = x256 == 0
        val x350 = if (x257) {
          val x258 = x245
          val x259 = x48(x258)
          val x260 = x247
          val x261 = x241(0)
          val x262 = x261 != x259
          val x263 = x260 || x262
          val x273 = if (x263) {
            val x264 = x244(0)
            val x265 = x52(x264) = x259
            val x266 = x244(1)
            val x267 = x56(x264) = x266
            val x268 = x241(0) = x259
            val x269 = x264 + 1
            val x270 = x244(0) = x269
            x247 = true
            ()
          } else {
            ()
          }
          x273
        } else {
          val x275 = x256 == 1
          val x348 = if (x275) {
            val x276 = x245
            val x277 = x49(x276)
            val x278 = x247
            val x279 = x241(1)
            val x280 = x279 != x277
            val x281 = x278 || x280
            val x291 = if (x281) {
              val x282 = x244(1)
              val x283 = x53(x282) = x277
              val x284 = x244(2)
              val x285 = x57(x282) = x284
              val x286 = x241(1) = x277
              val x287 = x282 + 1
              val x288 = x244(1) = x287
              x247 = true
              ()
            } else {
              ()
            }
            x291
          } else {
            val x293 = x256 == 2
            val x346 = if (x293) {
              val x294 = x245
              val x295 = x50(x294)
              val x296 = x247
              val x297 = x241(2)
              val x298 = x297 != x295
              val x299 = x296 || x298
              val x309 = if (x299) {
                val x300 = x244(2)
                val x301 = x54(x300) = x295
                val x302 = x244(3)
                val x303 = x58(x300) = x302
                val x304 = x241(2) = x295
                val x305 = x300 + 1
                val x306 = x244(2) = x305
                x247 = true
                ()
              } else {
                ()
              }
              x309
            } else {
              val x311 = x256 == 3
              val x344 = if (x311) {
                val x312 = x245
                val x313 = x51(x312)
                val x314 = x247
                val x315 = x241(3)
                val x316 = x315 != x313
                val x317 = x314 || x316
                val x325 = if (x317) {
                  val x318 = x244(3)
                  val x319 = x55(x318) = x313
                  val x320 = x241(3) = x313
                  val x321 = x318 + 1
                  val x322 = x244(3) = x321
                  x247 = true
                  ()
                } else {
                  ()
                }
                x325
              } else {
                val x327 = x245
                val x328 = x48(x327)
                val x329 = x247
                val x330 = x241(0)
                val x331 = x330 != x328
                val x332 = x329 || x331
                val x342 = if (x332) {
                  val x333 = x244(0)
                  val x334 = x52(x333) = x328
                  val x335 = x244(1)
                  val x336 = x56(x333) = x335
                  val x337 = x241(0) = x328
                  val x338 = x333 + 1
                  val x339 = x244(0) = x338
                  x247 = true
                  ()
                } else {
                  ()
                }
                x342
              }
              x344
            }
            x346
          }
          x348
        }
        val x351 = x246 += 1
        ()
      }
      val x354 = x245 += 1
      x246 = 0
      ()
    }
    val x389 = while ({val x358 = x246
      val x359 = x358 < 3
      x359}) {
      val x361 = x246
      val x362 = x244(x361)
      val x363 = x361 + 1
      val x364 = x244(x363)
      val x365 = x361 == 0
      val x385 = if (x365) {
        val x366 = x56(x362) = x364
        x366
      } else {
        val x368 = x361 == 1
        val x383 = if (x368) {
          val x369 = x57(x362) = x364
          x369
        } else {
          val x371 = x361 == 2
          val x381 = if (x371) {
            val x372 = x58(x362) = x364
            x372
          } else {
            val x374 = x361 == 3
            val x379 = if (x374) {
              val x375 = x59(x362) = x364
              x375
            } else {
              val x377 = x56(x362) = x364
              x377
            }
            x379
          }
          x381
        }
        x383
      }
      val x386 = x60(x361) = x362
      val x387 = x246 += 1
      ()
    }
    var x390: Int = 0
    val x391 = new Array[java.lang.String](4)
    var x392: Int = 0
    val x1240 = while ({val x393 = x390
      val x394 = x393 != -1
      x394}) {
      val x396 = x390
      val x397 = x16(x396)
      val x438 = if (x397) {
        val x398 = x30
        val x399 = x398 == 0
        val x404 = if (x399) {
          val x400 = x29(x398)
          val x401 = x15(x398)
          val x402 = x400 == x401
          x402
        } else {
          false
        }
        val x436 = if (x404) {
          true
        } else {
          val x433 = if (x399) {
            false
          } else {
            val x406 = x29(x398)
            val x407 = x398 - 1
            val x408 = x29(x407)
            val x410 = x407 == 0
            val x430 = if (x410) {
              val x409 = x408 + 1
              val x411 = x11(x409)
              x411
            } else {
              val x413 = x407 == 1
              val x428 = if (x413) {
                val x409 = x408 + 1
                val x414 = x12(x409)
                x414
              } else {
                val x416 = x407 == 2
                val x426 = if (x416) {
                  val x409 = x408 + 1
                  val x417 = x13(x409)
                  x417
                } else {
                  val x419 = x407 == 3
                  val x424 = if (x419) {
                    val x409 = x408 + 1
                    val x420 = x14(x409)
                    x420
                  } else {
                    val x409 = x408 + 1
                    val x422 = x11(x409)
                    x422
                  }
                  x424
                }
                x426
              }
              x428
            }
            val x431 = x406 == x430
            x431
          }
          val x434 = x433
          x434
        }
        x436
      } else {
        false
      }
      val x439 = x61(x396)
      val x480 = if (x439) {
        val x440 = x75
        val x441 = x440 == 0
        val x446 = if (x441) {
          val x442 = x74(x440)
          val x443 = x60(x440)
          val x444 = x442 == x443
          x444
        } else {
          false
        }
        val x478 = if (x446) {
          true
        } else {
          val x475 = if (x441) {
            false
          } else {
            val x448 = x74(x440)
            val x449 = x440 - 1
            val x450 = x74(x449)
            val x452 = x449 == 0
            val x472 = if (x452) {
              val x451 = x450 + 1
              val x453 = x56(x451)
              x453
            } else {
              val x455 = x449 == 1
              val x470 = if (x455) {
                val x451 = x450 + 1
                val x456 = x57(x451)
                x456
              } else {
                val x458 = x449 == 2
                val x468 = if (x458) {
                  val x451 = x450 + 1
                  val x459 = x58(x451)
                  x459
                } else {
                  val x461 = x449 == 3
                  val x466 = if (x461) {
                    val x451 = x450 + 1
                    val x462 = x59(x451)
                    x462
                  } else {
                    val x451 = x450 + 1
                    val x464 = x56(x451)
                    x464
                  }
                  x466
                }
                x468
              }
              x470
            }
            val x473 = x448 == x472
            x473
          }
          val x476 = x475
          x476
        }
        x478
      } else {
        false
      }
      val x481 = x438 || x480
      val x714 = if (x481) {
        val x482 = x392 -= 1
        val x485 = if (x397) {
          val x483 = x30 -= 1
          ()
        } else {
          ()
        }
        val x488 = if (x439) {
          val x486 = x75 -= 1
          ()
        } else {
          ()
        }
        val x489 = x390 -= 1
        val x490 = x390
        val x492 = x490 == -1
        val x508 = if (x492) {
          ()
        } else {
          val x493 = x16(x490)
          val x499 = if (x493) {
            val x494 = x30
            val x495 = x29(x494)
            val x496 = x495 + 1
            val x497 = x29(x494) = x496
            x497
          } else {
            ()
          }
          val x500 = x61(x490)
          val x506 = if (x500) {
            val x501 = x75
            val x502 = x74(x501)
            val x503 = x502 + 1
            val x504 = x74(x501) = x503
            x504
          } else {
            ()
          }
          ()
        }
        x508
      } else {
        val x511 = x396 == 3
        val x712 = if (x511) {
          var x512: java.lang.String = ""
          val x538 = if (x397) {
            val x513 = x30
            val x514 = x29(x513)
            val x515 = x513 == 0
            val x535 = if (x515) {
              val x516 = x7(x514)
              x516
            } else {
              val x518 = x513 == 1
              val x533 = if (x518) {
                val x519 = x8(x514)
                x519
              } else {
                val x521 = x513 == 2
                val x531 = if (x521) {
                  val x522 = x9(x514)
                  x522
                } else {
                  val x524 = x513 == 3
                  val x529 = if (x524) {
                    val x525 = x10(x514)
                    x525
                  } else {
                    val x527 = x7(x514)
                    x527
                  }
                  x529
                }
                x531
              }
              x533
            }
            x512 = x535
            ()
          } else {
            ()
          }
          val x564 = if (x439) {
            val x539 = x75
            val x540 = x74(x539)
            val x541 = x539 == 0
            val x561 = if (x541) {
              val x542 = x52(x540)
              x542
            } else {
              val x544 = x539 == 1
              val x559 = if (x544) {
                val x545 = x53(x540)
                x545
              } else {
                val x547 = x539 == 2
                val x557 = if (x547) {
                  val x548 = x54(x540)
                  x548
                } else {
                  val x550 = x539 == 3
                  val x555 = if (x550) {
                    val x551 = x55(x540)
                    x551
                  } else {
                    val x553 = x52(x540)
                    x553
                  }
                  x555
                }
                x557
              }
              x559
            }
            x512 = x561
            ()
          } else {
            ()
          }
          val x565 = x512
          val x566 = x392
          val x567 = x391(x566) = x565
          val x568 = x392 += 1
          var x569: Int = -1
          val x570 = x569 += 1
          val x571 = x569
          val x572 = x391(x571)
          val x573 = x569 += 1
          val x574 = x569
          val x575 = x391(x574)
          val x576 = x569 += 1
          val x577 = x569
          val x578 = x391(x577)
          val x579 = x569 += 1
          val x580 = x569
          val x581 = x391(x580)
          val x582 = printf("%s,%s,%s,%s\n",x572,x575,x578,x581)
          val x583 = x392 -= 1
          val x589 = if (x397) {
            val x584 = x30
            val x585 = x29(x584)
            val x586 = x585 + 1
            val x587 = x29(x584) = x586
            x587
          } else {
            ()
          }
          val x595 = if (x439) {
            val x590 = x75
            val x591 = x74(x590)
            val x592 = x591 + 1
            val x593 = x74(x590) = x592
            x593
          } else {
            ()
          }
          ()
        } else {
          var x597: java.lang.String = ""
          val x623 = if (x397) {
            val x598 = x30
            val x599 = x29(x598)
            val x600 = x598 == 0
            val x620 = if (x600) {
              val x601 = x7(x599)
              x601
            } else {
              val x603 = x598 == 1
              val x618 = if (x603) {
                val x604 = x8(x599)
                x604
              } else {
                val x606 = x598 == 2
                val x616 = if (x606) {
                  val x607 = x9(x599)
                  x607
                } else {
                  val x609 = x598 == 3
                  val x614 = if (x609) {
                    val x610 = x10(x599)
                    x610
                  } else {
                    val x612 = x7(x599)
                    x612
                  }
                  x614
                }
                x616
              }
              x618
            }
            x597 = x620
            ()
          } else {
            ()
          }
          val x649 = if (x439) {
            val x624 = x75
            val x625 = x74(x624)
            val x626 = x624 == 0
            val x646 = if (x626) {
              val x627 = x52(x625)
              x627
            } else {
              val x629 = x624 == 1
              val x644 = if (x629) {
                val x630 = x53(x625)
                x630
              } else {
                val x632 = x624 == 2
                val x642 = if (x632) {
                  val x633 = x54(x625)
                  x633
                } else {
                  val x635 = x624 == 3
                  val x640 = if (x635) {
                    val x636 = x55(x625)
                    x636
                  } else {
                    val x638 = x52(x625)
                    x638
                  }
                  x640
                }
                x642
              }
              x644
            }
            x597 = x646
            ()
          } else {
            ()
          }
          val x650 = x597
          val x651 = x392
          val x652 = x391(x651) = x650
          val x653 = x392 += 1
          val x681 = if (x397) {
            val x654 = x30
            val x656 = x29(x654)
            val x657 = x654 == 0
            val x677 = if (x657) {
              val x658 = x11(x656)
              x658
            } else {
              val x660 = x654 == 1
              val x675 = if (x660) {
                val x661 = x12(x656)
                x661
              } else {
                val x663 = x654 == 2
                val x673 = if (x663) {
                  val x664 = x13(x656)
                  x664
                } else {
                  val x666 = x654 == 3
                  val x671 = if (x666) {
                    val x667 = x14(x656)
                    x667
                  } else {
                    val x669 = x11(x656)
                    x669
                  }
                  x671
                }
                x673
              }
              x675
            }
            val x655 = x654 + 1
            val x678 = x29(x655) = x677
            val x679 = x30 += 1
            ()
          } else {
            ()
          }
          val x709 = if (x439) {
            val x682 = x75
            val x684 = x74(x682)
            val x685 = x682 == 0
            val x705 = if (x685) {
              val x686 = x56(x684)
              x686
            } else {
              val x688 = x682 == 1
              val x703 = if (x688) {
                val x689 = x57(x684)
                x689
              } else {
                val x691 = x682 == 2
                val x701 = if (x691) {
                  val x692 = x58(x684)
                  x692
                } else {
                  val x694 = x682 == 3
                  val x699 = if (x694) {
                    val x695 = x59(x684)
                    x695
                  } else {
                    val x697 = x56(x684)
                    x697
                  }
                  x699
                }
                x701
              }
              x703
            }
            val x683 = x682 + 1
            val x706 = x74(x683) = x705
            val x707 = x75 += 1
            ()
          } else {
            ()
          }
          val x710 = x390 += 1
          ()
        }
        x712
      }
      val x715 = x390
      val x717 = x715 == -1
      val x805 = if (x717) {
        false
      } else {
        val x718 = x16(x715)
        val x759 = if (x718) {
          val x719 = x30
          val x720 = x719 == 0
          val x725 = if (x720) {
            val x721 = x29(x719)
            val x722 = x15(x719)
            val x723 = x721 == x722
            x723
          } else {
            false
          }
          val x757 = if (x725) {
            true
          } else {
            val x754 = if (x720) {
              false
            } else {
              val x727 = x29(x719)
              val x728 = x719 - 1
              val x729 = x29(x728)
              val x731 = x728 == 0
              val x751 = if (x731) {
                val x730 = x729 + 1
                val x732 = x11(x730)
                x732
              } else {
                val x734 = x728 == 1
                val x749 = if (x734) {
                  val x730 = x729 + 1
                  val x735 = x12(x730)
                  x735
                } else {
                  val x737 = x728 == 2
                  val x747 = if (x737) {
                    val x730 = x729 + 1
                    val x738 = x13(x730)
                    x738
                  } else {
                    val x740 = x728 == 3
                    val x745 = if (x740) {
                      val x730 = x729 + 1
                      val x741 = x14(x730)
                      x741
                    } else {
                      val x730 = x729 + 1
                      val x743 = x11(x730)
                      x743
                    }
                    x745
                  }
                  x747
                }
                x749
              }
              val x752 = x727 == x751
              x752
            }
            val x755 = x754
            x755
          }
          x757
        } else {
          false
        }
        val x760 = x61(x715)
        val x801 = if (x760) {
          val x761 = x75
          val x762 = x761 == 0
          val x767 = if (x762) {
            val x763 = x74(x761)
            val x764 = x60(x761)
            val x765 = x763 == x764
            x765
          } else {
            false
          }
          val x799 = if (x767) {
            true
          } else {
            val x796 = if (x762) {
              false
            } else {
              val x769 = x74(x761)
              val x770 = x761 - 1
              val x771 = x74(x770)
              val x773 = x770 == 0
              val x793 = if (x773) {
                val x772 = x771 + 1
                val x774 = x56(x772)
                x774
              } else {
                val x776 = x770 == 1
                val x791 = if (x776) {
                  val x772 = x771 + 1
                  val x777 = x57(x772)
                  x777
                } else {
                  val x779 = x770 == 2
                  val x789 = if (x779) {
                    val x772 = x771 + 1
                    val x780 = x58(x772)
                    x780
                  } else {
                    val x782 = x770 == 3
                    val x787 = if (x782) {
                      val x772 = x771 + 1
                      val x783 = x59(x772)
                      x783
                    } else {
                      val x772 = x771 + 1
                      val x785 = x56(x772)
                      x785
                    }
                    x787
                  }
                  x789
                }
                x791
              }
              val x794 = x769 == x793
              x794
            }
            val x797 = x796
            x797
          }
          x799
        } else {
          false
        }
        val x802 = x759 || x801
        val x803 = !x802
        x803
      }
      val x1238 = if (x805) {
        var x806: java.lang.String = ""
        var x807: java.lang.String = ""
        val x1236 = while ({val x808 = x390
          val x809 = x16(x808)
          val x850 = if (x809) {
            val x810 = x30
            val x811 = x810 == 0
            val x816 = if (x811) {
              val x812 = x29(x810)
              val x813 = x15(x810)
              val x814 = x812 == x813
              x814
            } else {
              false
            }
            val x848 = if (x816) {
              true
            } else {
              val x845 = if (x811) {
                false
              } else {
                val x818 = x29(x810)
                val x819 = x810 - 1
                val x820 = x29(x819)
                val x822 = x819 == 0
                val x842 = if (x822) {
                  val x821 = x820 + 1
                  val x823 = x11(x821)
                  x823
                } else {
                  val x825 = x819 == 1
                  val x840 = if (x825) {
                    val x821 = x820 + 1
                    val x826 = x12(x821)
                    x826
                  } else {
                    val x828 = x819 == 2
                    val x838 = if (x828) {
                      val x821 = x820 + 1
                      val x829 = x13(x821)
                      x829
                    } else {
                      val x831 = x819 == 3
                      val x836 = if (x831) {
                        val x821 = x820 + 1
                        val x832 = x14(x821)
                        x832
                      } else {
                        val x821 = x820 + 1
                        val x834 = x11(x821)
                        x834
                      }
                      x836
                    }
                    x838
                  }
                  x840
                }
                val x843 = x818 == x842
                x843
              }
              val x846 = x845
              x846
            }
            x848
          } else {
            false
          }
          val x851 = x61(x808)
          val x892 = if (x851) {
            val x852 = x75
            val x853 = x852 == 0
            val x858 = if (x853) {
              val x854 = x74(x852)
              val x855 = x60(x852)
              val x856 = x854 == x855
              x856
            } else {
              false
            }
            val x890 = if (x858) {
              true
            } else {
              val x887 = if (x853) {
                false
              } else {
                val x860 = x74(x852)
                val x861 = x852 - 1
                val x862 = x74(x861)
                val x864 = x861 == 0
                val x884 = if (x864) {
                  val x863 = x862 + 1
                  val x865 = x56(x863)
                  x865
                } else {
                  val x867 = x861 == 1
                  val x882 = if (x867) {
                    val x863 = x862 + 1
                    val x868 = x57(x863)
                    x868
                  } else {
                    val x870 = x861 == 2
                    val x880 = if (x870) {
                      val x863 = x862 + 1
                      val x871 = x58(x863)
                      x871
                    } else {
                      val x873 = x861 == 3
                      val x878 = if (x873) {
                        val x863 = x862 + 1
                        val x874 = x59(x863)
                        x874
                      } else {
                        val x863 = x862 + 1
                        val x876 = x56(x863)
                        x876
                      }
                      x878
                    }
                    x880
                  }
                  x882
                }
                val x885 = x860 == x884
                x885
              }
              val x888 = x887
              x888
            }
            x890
          } else {
            false
          }
          val x893 = x850 || x892
          var x894: Boolean = x893
          val x895 = x893 == true
          val x1023 = if (x895) {
            false
          } else {
            x806 = ""
            var x897: java.lang.String = ""
            val x927 = if (x809) {
              val x898 = x30
              val x899 = x29(x898)
              val x900 = x898 == 0
              val x920 = if (x900) {
                val x901 = x7(x899)
                x901
              } else {
                val x903 = x898 == 1
                val x918 = if (x903) {
                  val x904 = x8(x899)
                  x904
                } else {
                  val x906 = x898 == 2
                  val x916 = if (x906) {
                    val x907 = x9(x899)
                    x907
                  } else {
                    val x909 = x898 == 3
                    val x914 = if (x909) {
                      val x910 = x10(x899)
                      x910
                    } else {
                      val x912 = x7(x899)
                      x912
                    }
                    x914
                  }
                  x916
                }
                x918
              }
              x897 = x920
              val x922 = x920 > ""
              val x925 = if (x922) {
                x806 = x920
                ()
              } else {
                ()
              }
              x925
            } else {
              ()
            }
            val x958 = if (x851) {
              val x928 = x75
              val x929 = x74(x928)
              val x930 = x928 == 0
              val x950 = if (x930) {
                val x931 = x52(x929)
                x931
              } else {
                val x933 = x928 == 1
                val x948 = if (x933) {
                  val x934 = x53(x929)
                  x934
                } else {
                  val x936 = x928 == 2
                  val x946 = if (x936) {
                    val x937 = x54(x929)
                    x937
                  } else {
                    val x939 = x928 == 3
                    val x944 = if (x939) {
                      val x940 = x55(x929)
                      x940
                    } else {
                      val x942 = x52(x929)
                      x942
                    }
                    x944
                  }
                  x946
                }
                x948
              }
              x897 = x950
              val x952 = x806
              val x953 = x950 > x952
              val x956 = if (x953) {
                x806 = x950
                ()
              } else {
                ()
              }
              x956
            } else {
              ()
            }
            val x959 = x806
            x807 = x959
            val x990 = if (x809) {
              val x961 = x30
              val x962 = x29(x961)
              val x963 = x961 == 0
              val x983 = if (x963) {
                val x964 = x7(x962)
                x964
              } else {
                val x966 = x961 == 1
                val x981 = if (x966) {
                  val x967 = x8(x962)
                  x967
                } else {
                  val x969 = x961 == 2
                  val x979 = if (x969) {
                    val x970 = x9(x962)
                    x970
                  } else {
                    val x972 = x961 == 3
                    val x977 = if (x972) {
                      val x973 = x10(x962)
                      x973
                    } else {
                      val x975 = x7(x962)
                      x975
                    }
                    x977
                  }
                  x979
                }
                x981
              }
              x897 = x983
              val x985 = x983 < x959
              val x988 = if (x985) {
                x807 = x983
                ()
              } else {
                ()
              }
              x988
            } else {
              ()
            }
            val x1021 = if (x851) {
              val x991 = x75
              val x992 = x74(x991)
              val x993 = x991 == 0
              val x1013 = if (x993) {
                val x994 = x52(x992)
                x994
              } else {
                val x996 = x991 == 1
                val x1011 = if (x996) {
                  val x997 = x53(x992)
                  x997
                } else {
                  val x999 = x991 == 2
                  val x1009 = if (x999) {
                    val x1000 = x54(x992)
                    x1000
                  } else {
                    val x1002 = x991 == 3
                    val x1007 = if (x1002) {
                      val x1003 = x55(x992)
                      x1003
                    } else {
                      val x1005 = x52(x992)
                      x1005
                    }
                    x1007
                  }
                  x1009
                }
                x1011
              }
              x897 = x1013
              val x1015 = x807
              val x1016 = x1013 < x1015
              val x1019 = if (x1016) {
                x807 = x1013
                ()
              } else {
                ()
              }
              x1019
            } else {
              ()
            }
            ()
          }
          val x1024 = x806
          val x1025 = x807
          val x1026 = x1024 != x1025
          x1026}) {
          val x1028 = x390
          val x1029 = x16(x1028)
          val x1131 = if (x1029) {
            val x1030 = x806
            val x1031 = x30
            val x1032 = x1031 == 0
            val x1033 = x1031 - 1
            val x1035 = x1033 == 0
            val x1038 = x1033 == 1
            val x1041 = x1033 == 2
            val x1044 = x1033 == 3
            val x1057 = if (x1032) {
              0
            } else {
              val x1034 = x29(x1033)
              val x1055 = if (x1035) {
                val x1036 = x11(x1034)
                x1036
              } else {
                val x1053 = if (x1038) {
                  val x1039 = x12(x1034)
                  x1039
                } else {
                  val x1051 = if (x1041) {
                    val x1042 = x13(x1034)
                    x1042
                  } else {
                    val x1049 = if (x1044) {
                      val x1045 = x14(x1034)
                      x1045
                    } else {
                      val x1047 = x11(x1034)
                      x1047
                    }
                    x1049
                  }
                  x1051
                }
                x1053
              }
              x1055
            }
            val x1080 = if (x1032) {
              val x1058 = x15(0)
              x1058
            } else {
              val x1060 = x29(x1033)
              val x1078 = if (x1035) {
                val x1061 = x1060 + 1
                val x1062 = x11(x1061)
                x1062
              } else {
                val x1076 = if (x1038) {
                  val x1061 = x1060 + 1
                  val x1064 = x12(x1061)
                  x1064
                } else {
                  val x1074 = if (x1041) {
                    val x1061 = x1060 + 1
                    val x1066 = x13(x1061)
                    x1066
                  } else {
                    val x1072 = if (x1044) {
                      val x1061 = x1060 + 1
                      val x1068 = x14(x1061)
                      x1068
                    } else {
                      val x1061 = x1060 + 1
                      val x1070 = x11(x1061)
                      x1070
                    }
                    x1072
                  }
                  x1074
                }
                x1076
              }
              x1078
            }
            var x1081: Int = x1057
            var x1082: Int = x1080
            val x1127 = while ({val x1083 = x1081
              val x1084 = x1082
              val x1085 = x1083 != x1084
              x1085}) {
              val x1087 = x30
              val x1088 = x1081
              val x1089 = x1082
              val x1092 = x1087 == 0
              val x1112 = if (x1092) {
                val x1090 = x1088 + x1089
                val x1091 = x1090 / 2
                val x1093 = x7(x1091)
                x1093
              } else {
                val x1095 = x1087 == 1
                val x1110 = if (x1095) {
                  val x1090 = x1088 + x1089
                  val x1091 = x1090 / 2
                  val x1096 = x8(x1091)
                  x1096
                } else {
                  val x1098 = x1087 == 2
                  val x1108 = if (x1098) {
                    val x1090 = x1088 + x1089
                    val x1091 = x1090 / 2
                    val x1099 = x9(x1091)
                    x1099
                  } else {
                    val x1101 = x1087 == 3
                    val x1106 = if (x1101) {
                      val x1090 = x1088 + x1089
                      val x1091 = x1090 / 2
                      val x1102 = x10(x1091)
                      x1102
                    } else {
                      val x1090 = x1088 + x1089
                      val x1091 = x1090 / 2
                      val x1104 = x7(x1091)
                      x1104
                    }
                    x1106
                  }
                  x1108
                }
                x1110
              }
              val x1113 = x1112 == x1030
              val x1125 = if (x1113) {
                val x1090 = x1088 + x1089
                val x1091 = x1090 / 2
                x1081 = x1091
                x1082 = x1091
                ()
              } else {
                val x1117 = x1112 < x1030
                val x1123 = if (x1117) {
                  val x1090 = x1088 + x1089
                  val x1091 = x1090 / 2
                  val x1118 = x1091 + 1
                  x1081 = x1118
                  ()
                } else {
                  val x1090 = x1088 + x1089
                  val x1091 = x1090 / 2
                  x1082 = x1091
                  ()
                }
                x1123
              }
              x1125
            }
            val x1128 = x1081
            val x1129 = x29(x1031) = x1128
            x1129
          } else {
            ()
          }
          val x1132 = x61(x1028)
          val x1234 = if (x1132) {
            val x1133 = x806
            val x1134 = x75
            val x1135 = x1134 == 0
            val x1136 = x1134 - 1
            val x1138 = x1136 == 0
            val x1141 = x1136 == 1
            val x1144 = x1136 == 2
            val x1147 = x1136 == 3
            val x1160 = if (x1135) {
              0
            } else {
              val x1137 = x74(x1136)
              val x1158 = if (x1138) {
                val x1139 = x56(x1137)
                x1139
              } else {
                val x1156 = if (x1141) {
                  val x1142 = x57(x1137)
                  x1142
                } else {
                  val x1154 = if (x1144) {
                    val x1145 = x58(x1137)
                    x1145
                  } else {
                    val x1152 = if (x1147) {
                      val x1148 = x59(x1137)
                      x1148
                    } else {
                      val x1150 = x56(x1137)
                      x1150
                    }
                    x1152
                  }
                  x1154
                }
                x1156
              }
              x1158
            }
            val x1183 = if (x1135) {
              val x1161 = x60(0)
              x1161
            } else {
              val x1163 = x74(x1136)
              val x1181 = if (x1138) {
                val x1164 = x1163 + 1
                val x1165 = x56(x1164)
                x1165
              } else {
                val x1179 = if (x1141) {
                  val x1164 = x1163 + 1
                  val x1167 = x57(x1164)
                  x1167
                } else {
                  val x1177 = if (x1144) {
                    val x1164 = x1163 + 1
                    val x1169 = x58(x1164)
                    x1169
                  } else {
                    val x1175 = if (x1147) {
                      val x1164 = x1163 + 1
                      val x1171 = x59(x1164)
                      x1171
                    } else {
                      val x1164 = x1163 + 1
                      val x1173 = x56(x1164)
                      x1173
                    }
                    x1175
                  }
                  x1177
                }
                x1179
              }
              x1181
            }
            var x1184: Int = x1160
            var x1185: Int = x1183
            val x1230 = while ({val x1186 = x1184
              val x1187 = x1185
              val x1188 = x1186 != x1187
              x1188}) {
              val x1190 = x75
              val x1191 = x1184
              val x1192 = x1185
              val x1195 = x1190 == 0
              val x1215 = if (x1195) {
                val x1193 = x1191 + x1192
                val x1194 = x1193 / 2
                val x1196 = x52(x1194)
                x1196
              } else {
                val x1198 = x1190 == 1
                val x1213 = if (x1198) {
                  val x1193 = x1191 + x1192
                  val x1194 = x1193 / 2
                  val x1199 = x53(x1194)
                  x1199
                } else {
                  val x1201 = x1190 == 2
                  val x1211 = if (x1201) {
                    val x1193 = x1191 + x1192
                    val x1194 = x1193 / 2
                    val x1202 = x54(x1194)
                    x1202
                  } else {
                    val x1204 = x1190 == 3
                    val x1209 = if (x1204) {
                      val x1193 = x1191 + x1192
                      val x1194 = x1193 / 2
                      val x1205 = x55(x1194)
                      x1205
                    } else {
                      val x1193 = x1191 + x1192
                      val x1194 = x1193 / 2
                      val x1207 = x52(x1194)
                      x1207
                    }
                    x1209
                  }
                  x1211
                }
                x1213
              }
              val x1216 = x1215 == x1133
              val x1228 = if (x1216) {
                val x1193 = x1191 + x1192
                val x1194 = x1193 / 2
                x1184 = x1194
                x1185 = x1194
                ()
              } else {
                val x1220 = x1215 < x1133
                val x1226 = if (x1220) {
                  val x1193 = x1191 + x1192
                  val x1194 = x1193 / 2
                  val x1221 = x1194 + 1
                  x1184 = x1221
                  ()
                } else {
                  val x1193 = x1191 + x1192
                  val x1194 = x1193 / 2
                  x1185 = x1194
                  ()
                }
                x1226
              }
              x1228
            }
            val x1231 = x1184
            val x1232 = x74(x1134) = x1231
            x1232
          } else {
            ()
          }
          ()
        }
        x1236
      } else {
        ()
      }
      x1238
    }
    ()
  }
}
/*****************************************
End of Generated Code
*******************************************/
