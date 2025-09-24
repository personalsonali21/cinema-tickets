package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketServiceImplTest {

    private TicketPaymentService paymentServiceMock;
    private SeatReservationService reservationServiceMock;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        paymentServiceMock = mock(TicketPaymentService.class);
        reservationServiceMock = mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(paymentServiceMock, reservationServiceMock);
    }

    @Test
    void testSuccessfulPurchaseWithAdultAndChild() {
        TicketTypeRequest adult = new TicketTypeRequest(Type.ADULT, 2);
        TicketTypeRequest child = new TicketTypeRequest(Type.CHILD, 1);

        ticketService.purchaseTickets(1L, adult, child);

        verify(paymentServiceMock).makePayment(1L, 65); 
        verify(reservationServiceMock).reserveSeat(1L, 3); 
    }

    @Test
    void testInfantDoesNotIncreaseSeatsOrCost() {
        TicketTypeRequest adult = new TicketTypeRequest(Type.ADULT, 1);
        TicketTypeRequest infant = new TicketTypeRequest(Type.INFANT, 2);

        ticketService.purchaseTickets(2L, adult, infant);

        verify(paymentServiceMock).makePayment(2L, 25); // only 1 adult
        verify(reservationServiceMock).reserveSeat(2L, 1); // only 1 adult seat
    }

    @Test
    void testChildWithoutAdultThrowsException() {
        TicketTypeRequest child = new TicketTypeRequest(Type.CHILD, 2);

        InvalidPurchaseException ex = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(3L, child)
        );

        assertTrue(ex.getMessage().contains("require at least one Adult"));
        verifyNoInteractions(paymentServiceMock);
        verifyNoInteractions(reservationServiceMock);
    }

    @Test
    void testExceedingMaxTicketsThrowsException() {
        TicketTypeRequest adult = new TicketTypeRequest(Type.ADULT, 26);

        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(4L, adult));

        assertTrue(ex.getMessage().contains("More than 25 tickets cannot be purchased at a time"));
        verifyNoInteractions(paymentServiceMock);
        verifyNoInteractions(reservationServiceMock);
    }

    @Test
    void testInvalidAccountThrowsException() {
        TicketTypeRequest adult = new TicketTypeRequest(Type.ADULT, 1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(0L, adult));

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(null, adult));

        verifyNoInteractions(paymentServiceMock);
        verifyNoInteractions(reservationServiceMock);
    }

    @Test
    void testEmptyRequestsThrowsException() {
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L));

        verifyNoInteractions(paymentServiceMock);
        verifyNoInteractions(reservationServiceMock);
    }
}