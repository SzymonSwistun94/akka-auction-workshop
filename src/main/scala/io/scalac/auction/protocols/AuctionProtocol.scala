package io.scalac.auction.protocols

// states

sealed trait AuctionState extends Product with Serializable
object AuctionState {

  /** [[Unscheduled]] ayction state
    *
    * Legal transitions: [[Scheduled]], [[InPreview]], [[InProgress]]
    */
  final case object Unscheduled extends AuctionState

  /** [[Scheduled]] ayction state
    *
    * Legal transitions: [[InPreview]], [[InProgress]]
    */
  final case object Scheduled extends AuctionState

  /** [[InPreview]] ayction state
    *
    * Legal transitions: [[Scheduled]], [[InProgress]]
    */
  final case object InPreview extends AuctionState

  /** [[InProgress]] ayction state
    *
    * Legal transitions: [[NearEnd]], [[Finished]]
    */
  final case object InProgress extends AuctionState

  /** [[NearEnd]] ayction state
    *
    * Legal transitions: [[Finished]]
    */
  final case object NearEnd extends AuctionState

  /** [[Finished]] ayction state
    *
    * Legal transitions: ()
    */
  final case object Finished extends AuctionState
}
