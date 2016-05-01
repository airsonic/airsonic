package net.sourceforge.subsonic.dao;

import java.util.List;

import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.Transcoding;

/**
 * Unit test of {@link TranscodingDao}.
 *
 * @author Sindre Mehus
 */
public class TranscodingDaoTestCase extends DaoTestCaseBase {

    @Override
    protected void setUp() throws Exception {
        getJdbcTemplate().execute("delete from transcoding2");
    }

    public void testCreateTranscoding() {
        Transcoding transcoding = new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", false);
        transcodingDao.createTranscoding(transcoding);

        Transcoding newTranscoding = transcodingDao.getAllTranscodings().get(0);
        assertTranscodingEquals(transcoding, newTranscoding);
    }

    public void testUpdateTranscoding() {
        Transcoding transcoding = new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", false);
        transcodingDao.createTranscoding(transcoding);
        transcoding = transcodingDao.getAllTranscodings().get(0);

        transcoding.setName("newName");
        transcoding.setSourceFormats("newSourceFormats");
        transcoding.setTargetFormat("newTargetFormats");
        transcoding.setStep1("newStep1");
        transcoding.setStep2("newStep2");
        transcoding.setStep3("newStep3");
        transcoding.setDefaultActive(true);
        transcodingDao.updateTranscoding(transcoding);

        Transcoding newTranscoding = transcodingDao.getAllTranscodings().get(0);
        assertTranscodingEquals(transcoding, newTranscoding);
    }

    public void testDeleteTranscoding() {
        assertEquals("Wrong number of transcodings.", 0, transcodingDao.getAllTranscodings().size());

        transcodingDao.createTranscoding(new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", true));
        assertEquals("Wrong number of transcodings.", 1, transcodingDao.getAllTranscodings().size());

        transcodingDao.createTranscoding(new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", true));
        assertEquals("Wrong number of transcodings.", 2, transcodingDao.getAllTranscodings().size());

        transcodingDao.deleteTranscoding(transcodingDao.getAllTranscodings().get(0).getId());
        assertEquals("Wrong number of transcodings.", 1, transcodingDao.getAllTranscodings().size());

        transcodingDao.deleteTranscoding(transcodingDao.getAllTranscodings().get(0).getId());
        assertEquals("Wrong number of transcodings.", 0, transcodingDao.getAllTranscodings().size());
    }

    public void testPlayerTranscoding() {
        Player player = new Player();
        playerDao.createPlayer(player);

        transcodingDao.createTranscoding(new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", false));
        transcodingDao.createTranscoding(new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", false));
        transcodingDao.createTranscoding(new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", false));
        Transcoding transcodingA = transcodingDao.getAllTranscodings().get(0);
        Transcoding transcodingB = transcodingDao.getAllTranscodings().get(1);
        Transcoding transcodingC = transcodingDao.getAllTranscodings().get(2);

        List<Transcoding> activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 0, activeTranscodings.size());

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[]{transcodingA.getId()});
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 1, activeTranscodings.size());
        assertTranscodingEquals(transcodingA, activeTranscodings.get(0));

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[]{transcodingB.getId(), transcodingC.getId()});
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 2, activeTranscodings.size());
        assertTranscodingEquals(transcodingB, activeTranscodings.get(0));
        assertTranscodingEquals(transcodingC, activeTranscodings.get(1));

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[0]);
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 0, activeTranscodings.size());
    }

    public void testCascadingDeletePlayer() {
        Player player = new Player();
        playerDao.createPlayer(player);

        transcodingDao.createTranscoding(new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", true));
        Transcoding transcoding = transcodingDao.getAllTranscodings().get(0);

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[]{transcoding.getId()});
        List<Transcoding> activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 1, activeTranscodings.size());

        playerDao.deletePlayer(player.getId());
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 0, activeTranscodings.size());
    }

    public void testCascadingDeleteTranscoding() {
        Player player = new Player();
        playerDao.createPlayer(player);

        transcodingDao.createTranscoding(new Transcoding(null, "name", "sourceFormats", "targetFormat", "step1", "step2", "step3", true));
        Transcoding transcoding = transcodingDao.getAllTranscodings().get(0);

        transcodingDao.setTranscodingsForPlayer(player.getId(), new int[]{transcoding.getId()});
        List<Transcoding> activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 1, activeTranscodings.size());

        transcodingDao.deleteTranscoding(transcoding.getId());
        activeTranscodings = transcodingDao.getTranscodingsForPlayer(player.getId());
        assertEquals("Wrong number of transcodings.", 0, activeTranscodings.size());
    }

    private void assertTranscodingEquals(Transcoding expected, Transcoding actual) {
        assertEquals("Wrong name.", expected.getName(), actual.getName());
        assertEquals("Wrong source formats.", expected.getSourceFormats(), actual.getSourceFormats());
        assertEquals("Wrong target format.", expected.getTargetFormat(), actual.getTargetFormat());
        assertEquals("Wrong step 1.", expected.getStep1(), actual.getStep1());
        assertEquals("Wrong step 2.", expected.getStep2(), actual.getStep2());
        assertEquals("Wrong step 3.", expected.getStep3(), actual.getStep3());
        assertEquals("Wrong default active.", expected.isDefaultActive(), actual.isDefaultActive());
    }
}
